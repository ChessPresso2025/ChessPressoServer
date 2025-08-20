package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.Lobby;
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

        // Setze die Callback-Funktion für den Code-Generator
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
        System.out.println("DEBUG: Creating private lobby for player: " + creatorId);

        // Prüfe erst, ob dieser Spieler bereits eine Lobby erstellt hat
        for (Lobby existingLobby : activeLobbies.values()) {
            if (existingLobby.getCreator().equals(creatorId) && existingLobby.isPrivate()) {
                System.out.println("DEBUG: Player already has a private lobby: " + existingLobby.getLobbyId());
                return existingLobby.getLobbyId(); // Gib die bestehende Lobby zurück
            }
        }

        String lobbyCode = lobbyCodeGenerator.generatePrivateLobbyCode();
        Lobby lobby = new Lobby(lobbyCode, LobbyType.PRIVATE, creatorId);
        activeLobbies.put(lobbyCode, lobby);

        System.out.println("DEBUG: Created new lobby with code: " + lobbyCode);
        System.out.println("DEBUG: Creator ID: " + creatorId);
        System.out.println("DEBUG: Active lobbies count after creation: " + activeLobbies.size());
        System.out.println("DEBUG: Lobby stored successfully: " + (activeLobbies.get(lobbyCode) != null));

        // Benachrichtige Creator
        messagingTemplate.convertAndSendToUser(creatorId, "/queue/lobby-created",
            Map.of("lobbyCode", lobbyCode, "message", "Private Lobby erstellt"));

        return lobbyCode;
    }


    public boolean joinPrivateLobby(String playerId, String lobbyCode) {
        System.out.println("DEBUG: Trying to join lobby with code: " + lobbyCode);
        System.out.println("DEBUG: Player ID: " + playerId);
        System.out.println("DEBUG: Active lobbies count: " + activeLobbies.size());

        Lobby lobby = activeLobbies.get(lobbyCode);

        if (lobby == null) {
            System.out.println("DEBUG: Lobby not found for code: " + lobbyCode);
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby nicht gefunden"));
            return false;
        }

        System.out.println("DEBUG: Lobby found. Current players: " + lobby.getPlayers());
        System.out.println("DEBUG: Lobby status: " + lobby.getStatus());
        System.out.println("DEBUG: Is lobby full: " + lobby.isFull());

        if (lobby.isFull()) {
            System.out.println("DEBUG: Lobby is full");
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby ist bereits voll"));
            return false;
        }

        if (lobby.getStatus() != LobbyStatus.WAITING) {
            System.out.println("DEBUG: Lobby status is not WAITING: " + lobby.getStatus());
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby ist nicht mehr verfügbar"));
            return false;
        }

        // Prüfe ob Spieler bereits in dieser Lobby ist
        if (lobby.getPlayers().contains(playerId)) {
            System.out.println("DEBUG: Player already in this lobby");
            return true; // Spieler ist bereits in der Lobby
        }

        System.out.println("DEBUG: Adding player to lobby");
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

        System.out.println("DEBUG: Player successfully added. New player count: " + lobby.getPlayers().size());
        System.out.println("DEBUG: Sent specific notifications to both players");
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


    public Lobby getLobby(String lobbyId) {
        return activeLobbies.get(lobbyId);
    }


    public Collection<Lobby> getAllLobbies() {
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
                }
            }
        }

        // Entferne leere Lobbys
        for (String lobbyId : lobbiesToRemove) {
            activeLobbies.remove(lobbyId);
        }
    }

    /**
     * Räumt verwaiste Lobbys auf (älter als 10 Minuten ohne Aktivität)
     */
    public void cleanupStaleLobbies() {
        List<String> lobbiesToRemove = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        for (Lobby lobby : activeLobbies.values()) {
            if (lobby.getCreatedAt().isBefore(cutoff) && !lobby.isGameStarted()) {
                lobbiesToRemove.add(lobby.getLobbyId());
                lobbyCodeGenerator.removeLobbyCode(lobby.getLobbyId());
            }
        }

        for (String lobbyId : lobbiesToRemove) {
            activeLobbies.remove(lobbyId);
        }
    }
}
