package org.example.chesspressoserver.service;

import lombok.Setter;
import org.example.chesspressoserver.controller.GameRestController;
import org.example.chesspressoserver.models.Lobby;
import org.example.chesspressoserver.models.LobbyStatus;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.models.requests.StartGameRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class LobbyService {
    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

    private final LobbyCodeGenerator lobbyCodeGenerator;
    private final SimpMessagingTemplate messagingTemplate;

    // Alle aktiven Lobbys
    private final Map<String, Lobby> activeLobbies = new ConcurrentHashMap<>();

    // Quick Match Warteschlangen nach GameTime
    private final Map<GameTime, Queue<String>> quickMatchQueues = new ConcurrentHashMap<>();
    private final UserService userService;

    // Setter für GameStartHandler (entkoppelt von GameRestController)
    @Setter
    private GameStartHandler gameStartHandler;

    // Konstanten für Messaging und Lobby-Attribute
    private static final String QUEUE_LOBBY_UPDATE = "/queue/lobby-update";
    private static final String QUEUE_LOBBY_ERROR = "/queue/lobby-error";
    private static final String TOPIC_LOBBY_PREFIX = "/topic/lobby/";
    private static final String LOBBY_ID_KEY = "lobbyId";
    private static final String PLAYERS_KEY = "players";
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String LOBBY_ERROR_TYPE = "LOBBY_ERROR";

    public LobbyService(LobbyCodeGenerator lobbyCodeGenerator, SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.lobbyCodeGenerator = lobbyCodeGenerator;
        this.messagingTemplate = messagingTemplate;

        // Setze die Callback-Funktion für den Code-Generator nur wenn die Methode existiert
        this.lobbyCodeGenerator.setLobbyExistsChecker(this::lobbyExists);

        // Initialisiere Quick Match Warteschlangen
        for (GameTime gameTime : Arrays.asList(GameTime.SHORT, GameTime.MIDDLE, GameTime.LONG)) {
            quickMatchQueues.put(gameTime, new LinkedList<>());
        }
        this.userService = userService;
    }


    private boolean lobbyExists(String lobbyCode) {
        return activeLobbies.containsKey(lobbyCode);
    }


    public String joinQuickMatch(String playerId, GameTime gameTime) {
        Queue<String> queue = quickMatchQueues.get(gameTime);

        if (queue.isEmpty()) {
            // Erster Spieler - erstelle neue Lobby und warte
            String lobbyId = lobbyCodeGenerator.generatePublicLobbyCode();
            Lobby lobby = new Lobby(lobbyId, Lobby.LobbyType.PUBLIC, playerId);
            lobby.setGameTime(gameTime);
            activeLobbies.put(lobbyId, lobby);
            queue.offer(lobbyId);

            messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_UPDATE,
                Map.of(
                    "type", "LOBBY_UPDATE",
                    LOBBY_ID_KEY, lobbyId,
                    PLAYERS_KEY, lobby.getPlayers(),
                    STATUS_KEY, "WAITING",
                    MESSAGE_KEY, "Suche nach Gegner..."
                ));

            return lobbyId;
        } else {
            // Zweiter Spieler - trete bestehender Lobby bei
            String lobbyId = queue.poll();
            Lobby lobby = activeLobbies.get(lobbyId);

            if (lobby != null && !lobby.isFull()) {
                lobby.addPlayer(playerId);
                lobby.setStatus(LobbyStatus.FULL);

                // Sende LOBBY_UPDATE an den zweiten Spieler, damit er subscriben kann
                messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_UPDATE,
                    Map.of(
                        "type", "LOBBY_UPDATE",
                        LOBBY_ID_KEY, lobbyId,
                        PLAYERS_KEY, lobby.getPlayers(),
                        STATUS_KEY, "FULL",
                        MESSAGE_KEY, "Quick Match gefunden. Du bist der zweite Spieler."
                    ));

                // Automatischer Spielstart, wenn zwei Spieler in der Lobby sind
                if (lobby.getPlayers().size() == 2 && gameStartHandler != null) {
                    StartGameRequest startReq = new StartGameRequest();
                    startReq.setLobbyId(lobby.getLobbyId());
                    startReq.setGameTime(lobby.getGameTime().name());
                    startReq.setRandomPlayers(true); // Quick-Match: Farben zufällig
                    // Spieler werden im GameStartHandler zufällig zugewiesen
                    new Thread(() -> {
                        try {
                            Thread.sleep(400); // 400ms Delay für Client-Subscription
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        gameStartHandler.startGame(startReq);
                    }).start();
                }
                return lobbyId;
            } else {
                // Fallback: Erstelle neue Lobby
                return joinQuickMatch(playerId, gameTime);
            }
        }
    }


    public String createPrivateLobby(String creatorId) {

        String lobbyCode = lobbyCodeGenerator.generatePrivateLobbyCode();
        Lobby lobby = new Lobby(lobbyCode, Lobby.LobbyType.PRIVATE, creatorId);
        activeLobbies.put(lobbyCode, lobby);

        return lobbyCode;
    }


    public boolean joinPrivateLobby(String playerId, String lobbyCode) {

        Lobby lobby = activeLobbies.get(lobbyCode);

        if (lobby == null) {
            messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_ERROR,
                Map.of(
                    "type", LOBBY_ERROR_TYPE,
                    MESSAGE_KEY, "Lobby nicht gefunden"
                ));
            return false;
        }

        if (lobby.isFull()) {
            messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_ERROR,
                Map.of(
                    "type", LOBBY_ERROR_TYPE,
                    MESSAGE_KEY, "Lobby ist bereits voll"
                ));
            return false;
        }

        if (lobby.getStatus() != LobbyStatus.WAITING) {
            messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_ERROR,
                Map.of(
                    "type", LOBBY_ERROR_TYPE,
                    MESSAGE_KEY, "Lobby ist nicht mehr verfügbar"
                ));
            return false;
        }

        // Prüfe ob Spieler bereits in dieser Lobby ist
        if (lobby.getPlayers().contains(playerId)) {
            return true; // Spieler ist bereits in der Lobby
        }

        lobby.addPlayer(playerId);
        lobby.setStatus(LobbyStatus.FULL);
        return true;
    }


    public void startGame(Lobby lobby) {
        lobby.setGameStarted(true);
        lobby.setStatus(LobbyStatus.IN_GAME);

        // Farben zuweisen
        assignColors(lobby);
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
        logger.info("Received leave request - Player: {}, Lobby: {}", playerId, lobbyId);

        Lobby lobby = activeLobbies.get(lobbyId);

        if (lobby == null) {
            logger.warn("Leave request failed - Lobby not found: {}", lobbyId);
            throw new IllegalArgumentException("Lobby nicht gefunden");
        }

        if (!lobby.getPlayers().contains(playerId)) {
            logger.warn("Leave request failed - Player {} not in lobby {}", playerId, lobbyId);
            throw new IllegalArgumentException("Spieler ist nicht in dieser Lobby");
        }

        boolean wasCreator = playerId.equals(lobby.getCreator());
        boolean wasInGame = lobby.isGameStarted();

        logger.debug("Leave context - wasCreator: {}, wasInGame: {}, playerCount: {}",
            wasCreator, wasInGame, lobby.getPlayers().size());

        // Entferne den Spieler
        lobby.removePlayer(playerId);

        if (wasInGame) {
            // Wenn das Spiel läuft, setze es zurück
            lobby.setGameStarted(false);
            logger.info("Game ended due to player leave - Lobby: {}", lobbyId);
        }

        if (lobby.getPlayers().isEmpty() || wasCreator) {
            // Wenn kein Spieler mehr da ist oder der Creator geht, schließe die Lobby
            activeLobbies.remove(lobbyId);
            lobbyCodeGenerator.removeLobbyCode(lobbyId);
            // Informiere alle über die Schließung der Lobby
            broadcastLobbyRemoved(lobbyId);
            logger.info("Lobby closed - Empty: {}, Creator left: {}, Lobby: {}",
                lobby.getPlayers().isEmpty(), wasCreator, lobbyId);
        } else {
            // Setze Status auf WAITING und benachrichtige verbleibende Spieler
            lobby.setStatus(LobbyStatus.WAITING);
            String message = wasInGame ?
                "Spieler hat während des Spiels verlassen - Spiel wurde beendet" :
                "Spieler hat Lobby verlassen";
            notifyLobbyUpdate(lobby, message);
            logger.info("Player left but lobby continues - Lobby: {}, Remaining players: {}",
                lobbyId, lobby.getPlayers().size());
        }
    }


    private void notifyLobbyUpdate(Lobby lobby, String message) {
        Map<String, Object> update = Map.of(
            LOBBY_ID_KEY, lobby.getLobbyId(),
            PLAYERS_KEY, lobby.getPlayers(),
            STATUS_KEY, lobby.getStatus(),
            MESSAGE_KEY, message
        );

        for (String playerId : lobby.getPlayers()) {
            messagingTemplate.convertAndSendToUser(playerId, QUEUE_LOBBY_UPDATE, update);
        }
    }




    /**
     * Erweiterte Methode: WebSocket-Broadcast bei Lobby-Join
     */
    public void broadcastLobbyJoined(String lobbyId, String newPlayerId) {
        Lobby lobby = activeLobbies.get(lobbyId);

        String playerName = userService.getUsernameById(newPlayerId);

        if (lobby != null) {
            Map<String, Object> updateMessage = Map.of(
                "type", "player-joined",
                LOBBY_ID_KEY, lobbyId,
                "newPlayerId", newPlayerId,
                PLAYERS_KEY, lobby.getPlayers(),
                STATUS_KEY, lobby.getStatus().toString(),
                MESSAGE_KEY, "Player " + playerName + " joined the lobby"
            );

            // Broadcast an alle Lobby-Teilnehmer
            messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobbyId, updateMessage);
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
                LOBBY_ID_KEY, lobbyId,
                "playerId", playerId,
                PLAYERS_KEY, lobby.getPlayers(),
                STATUS_KEY, lobby.getStatus().toString(),
                MESSAGE_KEY, "Player " + playerId + " left the lobby"
            );

            // Broadcast an alle verbleibenden Lobby-Teilnehmer
            messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobbyId, updateMessage);
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

    /**
     * Schliesst und entfernt eine Lobby explizit (z.B. nach Spielende).
     * Benachrichtigt auch alle Abonnenten ueber die Entfernung.
     */
    public void closeLobby(String lobbyId) {
        Lobby removed = activeLobbies.remove(lobbyId);
        if (removed != null) {
            // Lobby-Code freigeben
            lobbyCodeGenerator.removeLobbyCode(lobbyId);
            // Broadcast ueber Lobby-Entfernung
            broadcastLobbyRemoved(lobbyId);
            logger.info("Lobby closed after game end - Lobby: {}", lobbyId);
        } else {
            logger.debug("closeLobby called but lobby not found - Lobby: {}", lobbyId);
        }
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


    /**
     * Broadcast an alle, wenn eine Lobby entfernt wurde
     */
    private void broadcastLobbyRemoved(String lobbyId) {
        Map<String, Object> message = Map.of(
            "type", "LOBBY_REMOVED",
            LOBBY_ID_KEY, lobbyId,
            MESSAGE_KEY, "Die Lobby wurde geschlossen"
        );

        // Broadcast an den allgemeinen Lobby-Topic
        messagingTemplate.convertAndSend("/topic/lobbies", message);

        // Broadcast an den spezifischen Lobby-Topic
        messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobbyId, message);
    }

    /**
     * Startet ein Rematch in der bestehenden Lobby.
     * Setzt Status zurück, weist ggf. Farben neu zu und startet das Spiel.
     */
    public String startRematch(Lobby lobby) {

        //neue private lobby für die beiden Spieler erstellen (gegen Inkonsistenzen in der Datenbank)
        String newLobbyCode = createPrivateLobby(lobby.getWhitePlayer());
        joinPrivateLobby(lobby.getBlackPlayer(), newLobbyCode);

        return newLobbyCode;

    }
}
