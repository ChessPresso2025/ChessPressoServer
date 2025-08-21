package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.Lobby;
import org.example.chesspressoserver.models.Lobby.LobbyType;
import org.example.chesspressoserver.models.LobbyStatus;
import org.example.chesspressoserver.models.GameTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class LobbyService {

    private final LobbyCodeGenerator lobbyCodeGenerator;
    private final SimpMessagingTemplate messagingTemplate;

    // Alle aktiven Lobbys
    private final Map<String, Lobby> activeLobbies = new ConcurrentHashMap<>();

    // Quick Match Warteschlangen nach GameTime
    private final Map<GameTime, Queue<String>> quickMatchQueues = new ConcurrentHashMap<>();

    public LobbyService(LobbyCodeGenerator lobbyCodeGenerator, SimpMessagingTemplate messagingTemplate) {
        this.lobbyCodeGenerator = lobbyCodeGenerator;
        this.messagingTemplate = messagingTemplate;

        // Setze die Callback-Funktion für den Code-Generator nur wenn die Methode existiert
        this.lobbyCodeGenerator.setLobbyExistsChecker(this::lobbyExists);

        // Initialisiere Quick Match Warteschlangen
        for (GameTime gameTime : Arrays.asList(GameTime.SHORT, GameTime.MIDDLE, GameTime.LONG)) {
            quickMatchQueues.put(gameTime, new LinkedList<>());
        }
    }


    private boolean lobbyExists(String lobbyCode) {
        return activeLobbies.containsKey(lobbyCode);
    }


    public String joinQuickMatch(String playerId, GameTime gameTime) {
        Queue<String> queue = quickMatchQueues.get(gameTime);

        if (queue.isEmpty()) {
            // Erster Spieler - erstelle neue Lobby und warte
            String lobbyId = lobbyCodeGenerator.generatePublicLobbyCode();
            Lobby lobby = new Lobby(lobbyId, LobbyType.PUBLIC, playerId);
            lobby.setGameTime(gameTime);
            activeLobbies.put(lobbyId, lobby);
            queue.offer(lobbyId);

            // Benachrichtige Spieler über Warten
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-waiting",
                Map.of("lobbyId", lobbyId, "message", "Warte auf Gegner..."));

            return lobbyId;
        } else {
            // Zweiter Spieler - trete bestehender Lobby bei
            String lobbyId = queue.poll();
            Lobby lobby = activeLobbies.get(lobbyId);

            if (lobby != null && !lobby.isFull()) {
                lobby.addPlayer(playerId);
                lobby.setStatus(LobbyStatus.FULL);

                // Spiel kann starten - beide Spieler benachrichtigen
                startGame(lobby);
                return lobbyId;
            } else {
                // Fallback: Erstelle neue Lobby
                return joinQuickMatch(playerId, gameTime);
            }
        }
    }


    public String createPrivateLobby(String creatorId) {

        // Prüfe erst, ob dieser Spieler bereits eine Lobby erstellt hat
        for (Lobby existingLobby : activeLobbies.values()) {
            if (existingLobby.getCreator().equals(creatorId) && existingLobby.isPrivate()) {
                return existingLobby.getLobbyId(); // Gib die bestehende Lobby zurück
            }
        }

        String lobbyCode = lobbyCodeGenerator.generatePrivateLobbyCode();
        Lobby lobby = new Lobby(lobbyCode, LobbyType.PRIVATE, creatorId);
        activeLobbies.put(lobbyCode, lobby);



        // Benachrichtige Creator
        messagingTemplate.convertAndSendToUser(creatorId, "/queue/lobby-created",
            Map.of("lobbyCode", lobbyCode, "message", "Private Lobby erstellt"));

        return lobbyCode;
    }


    public boolean joinPrivateLobby(String playerId, String lobbyCode) {

        Lobby lobby = activeLobbies.get(lobbyCode);

        if (lobby == null) {
            System.out.println("DEBUG: Lobby not found for code: " + lobbyCode);
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby nicht gefunden"));
            return false;
        }

        if (lobby.isFull()) {
            System.out.println("DEBUG: Lobby is full");
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby ist bereits voll"));
            return false;
        }

        if (lobby.getStatus() != LobbyStatus.WAITING) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby ist nicht mehr verfügbar"));
            return false;
        }

        // Prüfe ob Spieler bereits in dieser Lobby ist
        if (lobby.getPlayers().contains(playerId)) {
            return true; // Spieler ist bereits in der Lobby
        }

        lobby.addPlayer(playerId);
        lobby.setStatus(LobbyStatus.FULL);

        // Spezifische Benachrichtigungen für beide Spieler
        String creatorId = lobby.getCreator();

        // Benachrichtige den Creator über den neuen Spieler
        messagingTemplate.convertAndSendToUser(creatorId, "/queue/player-joined", Map.of(
            "lobbyId", lobby.getLobbyId(),
            "newPlayerId", playerId,
            "players", lobby.getPlayers(),
            "status", lobby.getStatus().toString(),
            "message", "Ein Spieler ist deiner Lobby beigetreten!",
            "isLobbyFull", true
        ));

        // Benachrichtige den beitretenden Spieler über erfolgreichen Beitritt
        messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-joined", Map.of(
            "lobbyId", lobby.getLobbyId(),
            "creatorId", creatorId,
            "players", lobby.getPlayers(),
            "status", lobby.getStatus().toString(),
            "message", "Erfolgreich der Lobby beigetreten!",
            "isLobbyFull", true
        ));

        return true;
    }


    public boolean configurePrivateLobby(String playerId, String lobbyCode, GameTime gameTime,
                                       String whitePlayer, String blackPlayer, boolean randomColors) {
        Lobby lobby = activeLobbies.get(lobbyCode);

        if (lobby == null || !lobby.getCreator().equals(playerId)) {
            return false;
        }

        lobby.setGameTime(gameTime);
        lobby.setWhitePlayer(whitePlayer);
        lobby.setBlackPlayer(blackPlayer);
        lobby.setRandomColors(randomColors);

        // Wenn Lobby voll ist und konfiguriert, kann Spiel starten
        if (lobby.canStart()) {
            startGame(lobby);
        } else {
            notifyLobbyUpdate(lobby, "Lobby konfiguriert");
        }

        return true;
    }


    private void startGame(Lobby lobby) {
        lobby.setGameStarted(true);
        lobby.setStatus(LobbyStatus.IN_GAME);

        // Farben zuweisen
        assignColors(lobby);

        // Beide Spieler zum Lobby-Channel hinzufügen und Spiel starten
        String lobbyChannel = "/topic/lobby/" + lobby.getLobbyId();

        for (String playerId : lobby.getPlayers()) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/game-start", Map.of(
                "lobbyId", lobby.getLobbyId(),
                "gameTime", lobby.getGameTime().name(),
                "whitePlayer", lobby.getWhitePlayer(),
                "blackPlayer", lobby.getBlackPlayer(),
                "lobbyChannel", lobbyChannel
            ));
        }

        // Spiel-Start Nachricht an Lobby-Channel
        messagingTemplate.convertAndSend(lobbyChannel, Map.of(
            "type", "GAME_START",
            "players", lobby.getPlayers(),
            "whitePlayer", lobby.getWhitePlayer(),
            "blackPlayer", lobby.getBlackPlayer(),
            "gameTime", lobby.getGameTime()
        ));
    }


    private void assignColors(Lobby lobby) {
        List<String> players = lobby.getPlayers();

        if (lobby.isPrivate() && !lobby.isRandomColors()) {
            // Farben wurden bereits vom Creator festgelegt
            return;
        }

        // Zufällige Farbzuweisung
        Collections.shuffle(players);
        lobby.setWhitePlayer(players.get(0));
        lobby.setBlackPlayer(players.get(1));
    }


    public void leaveLobby(String playerId, String lobbyId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby != null) {
            lobby.removePlayer(playerId);

            if (lobby.getPlayers().isEmpty()) {
                // Lobby schließen
                activeLobbies.remove(lobbyId);
                lobbyCodeGenerator.removeLobbyCode(lobbyId);
            } else {
                // Andere Spieler benachrichtigen
                notifyLobbyUpdate(lobby, "Spieler hat Lobby verlassen");
            }
        }
    }


    private void notifyLobbyUpdate(Lobby lobby, String message) {
        Map<String, Object> update = Map.of(
            "lobbyId", lobby.getLobbyId(),
            "players", lobby.getPlayers(),
            "status", lobby.getStatus(),
            "message", message
        );

        for (String playerId : lobby.getPlayers()) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-update", update);
        }
    }

    /**
     * Neue Methode: Update Player Ready Status für WebSocket-Integration
     */
    public boolean updatePlayerReadyStatus(String lobbyId, String playerId, boolean ready) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null || !lobby.getPlayers().contains(playerId)) {
            return false;
        }

        lobby.setPlayerReady(playerId, ready);
        return true;
    }

    /**
     * Neue Methode: Prüfe ob alle Spieler bereit sind
     */
    public boolean areAllPlayersReady(String lobbyId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null || lobby.getPlayers().size() < 2) {
            return false;
        }

        return lobby.areAllPlayersReady();
    }

    /**
     * Neue Methode: Starte Spiel wenn alle bereit sind
     */
    public void startGameIfReady(String lobbyId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby != null && areAllPlayersReady(lobbyId) && lobby.getPlayers().size() >= 2) {
            startGame(lobby);
        }
    }

    /**
     * Erweiterte Methode: WebSocket-Broadcast bei Lobby-Join
     */
    public void broadcastLobbyJoined(String lobbyId, String newPlayerId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby != null) {
            Map<String, Object> updateMessage = Map.of(
                "type", "player-joined",
                "lobbyId", lobbyId,
                "newPlayerId", newPlayerId,
                "players", lobby.getPlayers(),
                "status", lobby.getStatus().toString(),
                "message", "Player " + newPlayerId + " joined the lobby"
            );

            // Broadcast an alle Lobby-Teilnehmer
            messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, updateMessage);
        }
    }

    /**
     * Erweiterte Methode: WebSocket-Broadcast bei Lobby-Leave
     */
    public void broadcastPlayerLeft(String lobbyId, String playerId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby != null) {
            Map<String, Object> updateMessage = Map.of(
                "type", "player-left",
                "lobbyId", lobbyId,
                "playerId", playerId,
                "players", lobby.getPlayers(),
                "status", lobby.getStatus().toString(),
                "message", "Player " + playerId + " left the lobby"
            );

            // Broadcast an alle verbleibenden Lobby-Teilnehmer
            messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, updateMessage);
        }
    }

    /**
     * Erweiterte Methode: WebSocket-Broadcast bei Game-Start
     */
    public void broadcastGameStart(String lobbyId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby != null && lobby.isGameStarted()) {
            Map<String, Object> gameStartMessage = Map.of(
                "type", "game-start",
                "lobbyId", lobbyId,
                "gameTime", lobby.getGameTime() != null ? lobby.getGameTime().toString() : "MIDDLE",
                "whitePlayer", lobby.getWhitePlayer(),
                "blackPlayer", lobby.getBlackPlayer(),
                "lobbyChannel", "game-" + lobbyId
            );

            // Broadcast an alle Lobby-Teilnehmer
            messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, gameStartMessage);
        }
    }

    public Lobby getLobby(String lobbyId) {
        return activeLobbies.get(lobbyId);
    }

    public Collection<Lobby> getAllLobbies() {
        return activeLobbies.values();
    }

    public Collection<Lobby> getAllActiveLobbies() {
        return activeLobbies.values();
    }

    public String getPlayerLobby(String playerId) {
        for (Lobby lobby : activeLobbies.values()) {
            if (lobby.getPlayers().contains(playerId)) {
                return lobby.getLobbyId();
            }
        }
        return null;
    }

    public void forceLeaveAllLobbies(String playerId) {
        List<String> lobbiesToRemove = new ArrayList<>();

        for (Lobby lobby : activeLobbies.values()) {
            if (lobby.getPlayers().contains(playerId)) {
                lobby.removePlayer(playerId);

                if (lobby.getPlayers().isEmpty()) {
                    lobbiesToRemove.add(lobby.getLobbyId());
                    lobbyCodeGenerator.removeLobbyCode(lobby.getLobbyId());
                } else {
                    // Benachrichtige andere Spieler
                    notifyLobbyUpdate(lobby, "Spieler hat Lobby verlassen (Verbindung getrennt)");

                    // Broadcast WebSocket-Update
                    broadcastPlayerLeft(lobby.getLobbyId(), playerId);
                }
            }
        }

        // Entferne leere Lobbys
        for (String lobbyId : lobbiesToRemove) {
            activeLobbies.remove(lobbyId);
        }
    }

    public void cleanupStaleLobbies() {
        List<String> staleLobbyIds = new ArrayList<>();
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        for (Map.Entry<String, Lobby> entry : activeLobbies.entrySet()) {
            Lobby lobby = entry.getValue();
            if (lobby.getCreatedAt().isBefore(cutoffTime) && lobby.getPlayers().isEmpty()) {
                staleLobbyIds.add(entry.getKey());
            }
        }

        for (String lobbyId : staleLobbyIds) {
            activeLobbies.remove(lobbyId);
            lobbyCodeGenerator.removeLobbyCode(lobbyId);
        }
    }
}
