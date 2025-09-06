package org.example.chesspressoserver.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.chesspressoserver.dto.*;
import org.example.chesspressoserver.gamelogic.GameController;
import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.*;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.PieceType;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;
import org.example.chesspressoserver.models.requests.*;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.StatsService;
import org.example.chesspressoserver.service.UserService;
import org.example.chesspressoserver.repository.GameRepository;
import org.example.chesspressoserver.service.GameStartHandler;

import java.security.Principal;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class GameRestController implements GameStartHandler {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final UserService userService;
    private final GameRepository gameRepository;
    private final StatsService statsService;
    private final SimpUserRegistry simpUserRegistry;

    private static final Logger logger = LoggerFactory.getLogger(GameRestController.class);

    @Autowired
    public GameRestController(GameManager gameManager, SimpMessagingTemplate messagingTemplate, LobbyService lobbyService, UserService userService, GameRepository gameRepository, StatsService statsService, SimpUserRegistry simpUserRegistry) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.gameRepository = gameRepository;
        this.statsService = statsService;
        this.simpUserRegistry = simpUserRegistry;
    }

    private static final String TOPIC_LOBBY_PREFIX = "/topic/lobby/";
    private static final String LOBBY_ID = "lobbyId";

    @Override
    public void startGame(StartGameRequest request) {
        logger.info("Received start request: {}", request);
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            // Rückgabe für REST, für Interface-Call ignorieren
            return;
        }
        Lobby lobby = lobbyService.getLobby(request.getLobbyId());
        if (lobby == null) {
            return;
        }
        if (request.getGameTime() != null) {
            try {
                lobby.setGameTime(GameTime.valueOf(request.getGameTime()));
            } catch (IllegalArgumentException e) {
                return;
            }
        }
        lobby.setRandomColors(request.isRandomPlayers());
        lobbyService.startGame(lobby);
        Optional<User> whiteUserOpt = userService.getUserById(lobby.getWhitePlayer());
        Optional<User> blackUserOpt = userService.getUserById(lobby.getBlackPlayer());
        if (whiteUserOpt.isEmpty() || blackUserOpt.isEmpty()) {
            return;
        }
        UUID whitePlayerId = whiteUserOpt.get().getId();
        UUID blackPlayerId = blackUserOpt.get().getId();
        String whitePlayerName = whiteUserOpt.get().getUsername();
        String blackPlayerName = blackUserOpt.get().getUsername();
        GameEntity gameEntity = new GameEntity();
        gameEntity.setWhitePlayerId(whitePlayerId);
        gameEntity.setBlackPlayerId(blackPlayerId);
        gameEntity.setStartedAt(OffsetDateTime.now());
        gameEntity.setLobbyId(request.getLobbyId());
        gameRepository.save(gameEntity);
        gameManager.startGame(request.getLobbyId(), gameEntity.getId());
        messagingTemplate.convertAndSend(
                TOPIC_LOBBY_PREFIX + request.getLobbyId(),
                Map.of(
                        "type", "gameStarted",
                        "success", true,
                        LOBBY_ID, request.getLobbyId(),
                        "gameTime", request.getGameTime(),
                        "whitePlayer", whitePlayerName,
                        "blackPlayer", blackPlayerName,
                        "randomPlayers", request.isRandomPlayers(),
                        "board", getBoardForLobby(request.getLobbyId())
                )
        );
    }

    private Map<String, PieceInfo> getBoardForLobby(String lobbyId) {
        GameController gameController = gameManager.getGameByLobby(lobbyId);
        Map<String, PieceInfo> boardMap = new HashMap<>();
        if (gameController == null) return boardMap;
        Board board = gameController.getBoard();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Position pos = new Position(x, y);
                ChessPiece piece = board.getPiece(y, x);
                if (piece != null) {
                    boardMap.put(pos.toString(), new PieceInfo(piece.getType(), piece.getColour()));
                } else {
                    boardMap.put(pos.toString(), new PieceInfo(PieceType.NULL, TeamColor.NULL));
                }
            }
        }
        return boardMap;
    }

    @MessageMapping("/game/end")
    public void handleGameEnd(@Payload GameEndMessage message) {
        if (message.getLobbyId() == null || message.getLobbyId().isEmpty() || message.getPlayer() == null || message.getPlayer() == TeamColor.NULL) {
            return;
        }
        Lobby lobby = lobbyService.getLobby(message.getLobbyId());
        if (lobby == null) {
            return;
        }
        EndType endType = message.endType;

        switch (endType) {
            case RESIGNATION:
                onResign(message, lobby);
                break;
            case AGREED_DRAW:
                onDraw(message, lobby);
                break;
            case TIMEOUT:
                onTimeout(message, lobby);
                break;
            case CHECKMATE:
                onCheckmate(message, lobby);
                break;
        }
    }

    private void updateGameEndInDatabase(String lobbyId, String result) {
        Optional<GameEntity> gameOpt = gameRepository.findByLobbyId(lobbyId);
        if (gameOpt.isPresent()) {
            GameEntity game = gameOpt.get();
            game.setEndedAt(OffsetDateTime.now());
            game.setResult(result);
            gameRepository.save(game);
        }
    }

    private void handleGameEndCommon(GameEndMessage message, Lobby lobby, EndType endType, boolean callResignGame) {
        // Spieler-IDs und User-Objekte ermitteln
        Optional<User> whiteUserOpt = userService.getUserById(lobby.getWhitePlayer());
        Optional<User> blackUserOpt = userService.getUserById(lobby.getBlackPlayer());
        UUID whiteId = whiteUserOpt.map(User::getId).orElse(null);
        UUID blackId = blackUserOpt.map(User::getId).orElse(null);
        logger.info("weißer spieler: {}  schwarzer spieler: {}", (whiteId != null ? whiteId.toString() : null), (blackId != null ? blackId.toString() : null) );

        boolean draw = endType == EndType.AGREED_DRAW;
        boolean success = true;
        String result = getResultString(endType, message, lobby);
        String reason = getReasonString(endType);
        String winner = null;
        String loser = null;

        if (draw) {
            reportDrawStats(whiteId, blackId);
        } else {
            if (callResignGame) {
                success = gameManager.resignGame(message.getLobbyId());
            }
            winner = getWinnerId(message, lobby);
            loser = getLoserId(message, lobby);
            reportWinLossStats(message, whiteId, blackId);
        }

        updateGameEndInDatabase(message.getLobbyId(), result);
        gameManager.removeGameByLobbyId(message.getLobbyId());

        sendGameEndMessage(endType, callResignGame, success, draw, winner, loser, lobby, reason, message);
    }

    private String getResultString(EndType endType, GameEndMessage message, Lobby lobby) {
        return switch (endType) {
            case AGREED_DRAW -> "DRAW";
            case RESIGNATION -> message.getPlayer() == TeamColor.WHITE ? "BLACK_WIN" : "WHITE_WIN";
            case TIMEOUT -> message.getPlayer() == TeamColor.WHITE ? "BLACK_WIN" : "WHITE_WIN";
            case CHECKMATE -> message.getPlayer() == TeamColor.WHITE ? "BLACK_WIN" : "WHITE_WIN";
            default -> "UNKNOWN";
        };
    }

    private String getReasonString(EndType endType) {
        return switch (endType) {
            case RESIGNATION -> "RESIGN";
            case TIMEOUT -> "TIMEOUT";
            case CHECKMATE -> "CHECKMATE";
            case AGREED_DRAW -> "DRAW";
            default -> "UNKNOWN";
        };
    }

    private String getWinnerId(GameEndMessage message, Lobby lobby) {
        return message.getPlayer() == TeamColor.WHITE ? lobby.getBlackPlayer() : lobby.getWhitePlayer();
    }

    private String getLoserId(GameEndMessage message, Lobby lobby) {
        return message.getPlayer() == TeamColor.WHITE ? lobby.getWhitePlayer() : lobby.getBlackPlayer();
    }

    private void reportDrawStats(UUID whiteId, UUID blackId) {
        if (whiteId != null) {
            StatsReportRequest whiteDraw = new StatsReportRequest();
            whiteDraw.setResult("DRAW");
            statsService.reportGameResult(whiteId, whiteDraw);
        }
        if (blackId != null) {
            StatsReportRequest blackDraw = new StatsReportRequest();
            blackDraw.setResult("DRAW");
            statsService.reportGameResult(blackId, blackDraw);
        }
    }

    private void reportWinLossStats(GameEndMessage message, UUID whiteId, UUID blackId) {
        if (message.getPlayer() == TeamColor.WHITE) {
            if (whiteId != null) {
                StatsReportRequest whiteLoss = new StatsReportRequest();
                whiteLoss.setResult("LOSS");
                statsService.reportGameResult(whiteId, whiteLoss);
            }
            if (blackId != null) {
                StatsReportRequest blackWin = new StatsReportRequest();
                blackWin.setResult("WIN");
                statsService.reportGameResult(blackId, blackWin);
            }
        } else {
            if (whiteId != null) {
                StatsReportRequest whiteWin = new StatsReportRequest();
                whiteWin.setResult("WIN");
                statsService.reportGameResult(whiteId, whiteWin);
            }
            if (blackId != null) {
                StatsReportRequest blackLoss = new StatsReportRequest();
                blackLoss.setResult("LOSS");
                statsService.reportGameResult(blackId, blackLoss);
            }
        }
    }

    private void sendGameEndMessage(EndType endType, boolean callResignGame, boolean success, boolean draw, String winner, String loser, Lobby lobby, String reason, GameEndMessage message) {
        if (endType == EndType.AGREED_DRAW || !callResignGame || success) {
            messagingTemplate.convertAndSend(
                    TOPIC_LOBBY_PREFIX + message.getLobbyId(),
                    new GameEndResponse(
                            draw ? null : userService.getUsernameById(winner),
                            draw ? null : userService.getUsernameById(loser),
                            draw,
                            lobby.getLobbyId(),
                            reason
                    )
            );
        } else {
            messagingTemplate.convertAndSend(
                    TOPIC_LOBBY_PREFIX + message.getLobbyId(),
                    Map.of(
                            "type", "gameResigned",
                            LOBBY_ID, message.getLobbyId(),
                            "player", message.getPlayer(),
                            "success", false,
                            "error", "Ungültige Lobby-ID oder Aufgabe nicht möglich"
                    )
            );
        }
    }

    private void onTimeout(GameEndMessage message, Lobby lobby) {
        handleGameEndCommon(message, lobby, EndType.TIMEOUT, false);
    }

    private void onDraw(GameEndMessage message, Lobby lobby) {
        handleGameEndCommon(message, lobby, EndType.AGREED_DRAW, false);
    }

    private void onResign(GameEndMessage message, Lobby lobby) {
        handleGameEndCommon(message, lobby, EndType.RESIGNATION, true);
    }

    private void onCheckmate(GameEndMessage message, Lobby lobby) {
        handleGameEndCommon(message, lobby, EndType.CHECKMATE, false);
    }

    @MessageMapping("/lobby/{lobbyId}/remis")
    public void handleRemisMessage(@Payload RemisMessage remisMessage) {
        if (remisMessage.getLobbyId() == null || remisMessage.getLobbyId().isEmpty() || remisMessage.getRequester() == null) {
            return;
        }
        Lobby lobby = lobbyService.getLobby(remisMessage.getLobbyId());
        if (lobby == null) {
            return;
        }
        // Remis-Angebot: responder == null
        if (remisMessage.getResponder() == TeamColor.NULL) {
            String sender = remisMessage.getRequester() == TeamColor.WHITE ? lobby.getWhitePlayer() : lobby.getBlackPlayer();
            String receiver = remisMessage.getRequester() == TeamColor.WHITE ? lobby.getBlackPlayer() : lobby.getWhitePlayer();
            String receiver_name = userService.getUsernameById(receiver);
            messagingTemplate.convertAndSend(
                TOPIC_LOBBY_PREFIX + "remis/" + receiver_name,
                Map.of(
                    "type", "remis",
                    "requester", remisMessage.getRequester(),
                    "accept", false,
                    LOBBY_ID, remisMessage.getLobbyId()
                )
            );
        } else if (remisMessage.isAccept()) {

            GameEndMessage endMessage = new GameEndMessage(
                remisMessage.getLobbyId(),
                remisMessage.getResponder(), // Der Spieler, der das Remis bestätigt
                EndType.AGREED_DRAW
            );
            handleGameEnd(endMessage);
        }
    }

    @AllArgsConstructor
    @Data
    public static class GameEndMessage {
        private String lobbyId;
        private TeamColor player;
        private EndType endType;
    }

    @Data
    public static class GameEndResponse {
        private String winner;
        private String loser;
        private boolean draw;
        private String lobbyId;
        private final String type = "game-end";
        private String reason;

        public GameEndResponse(String winner, String loser, boolean draw, String lobbyId, String reason) {
            this.winner = winner;
            this.loser = loser;
            this.draw = draw;
            this.lobbyId = lobbyId;
            this.reason = reason;
        }
    }

    @MessageMapping("/lobby/rematch/request")
    public void handleRematchRequest(@Payload RematchRequest request, Principal principal) {
        Lobby lobby = lobbyService.getLobby(request.getLobbyId());
        if (lobby == null) return;
        List<String> players = lobby.getPlayers();
        if (players.size() != 2) return;
        String fromPlayerName = request.getPlayerId();
        String fromPlayerId = userService.getUserByUsername(fromPlayerName).get().getId().toString();
        String toPlayerId = players.stream().filter(id -> !id.equals(fromPlayerId)). findFirst().orElse(null);
        if (toPlayerId == null) return;
        logger.info("[Rematch] Principal: {}, fromPlayerId: {}, toPlayerId: {}", (principal != null ? principal.getName() : "null"), fromPlayerId, toPlayerId);
        // Aktive User-Principals loggen
        logger.info("[Rematch] Aktive User im SimpUserRegistry:");
        simpUserRegistry.getUsers().forEach(user -> {
            logger.info("  User: '{}' Sessions: {}", user.getName(), user.getSessions().size());
            user.getSessions().forEach(session -> logger.info("    SessionId: {}", session.getId()));
        });
        // Sende RematchOffer an das Lobby-Topic, Empfänger im Payload
        RematchOffer offer = new RematchOffer(
            lobby.getLobbyId(), fromPlayerId, userService.getUsernameById(toPlayerId));
        messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobby.getLobbyId() + "/rematch-offer", offer);
        logger.info("[Rematch] Sent offer to topic: {}", TOPIC_LOBBY_PREFIX + lobby.getLobbyId() + "/rematch-offer");
    }

    @MessageMapping("/lobby/rematch/response")
    public void handleRematchResponse(@Payload RematchResponse response) {
        Lobby lobby = lobbyService.getLobby(response.getLobbyId());
        if (lobby == null) return;
        List<String> players = lobby.getPlayers();
        if (players.size() != 2) return;
        String fromPlayerName = response.getPlayerId(); // Username!
        String toPlayerName = players.stream().filter(name -> !name.equals(fromPlayerName)).findFirst().orElse(null);
        if (toPlayerName == null) return;

        // Wenn akzeptiert, neue Partie starten
        if ("accepted".equalsIgnoreCase(response.getResponse())) {
            // Sende RematchResult an beide Spieler (per Username)
            String newLobbyCode = lobbyService.startRematch(lobby);
            RematchResult result = new RematchResult(lobby.getLobbyId(), response.getResponse(), newLobbyCode);
            //schicke rematch-antwort mit neuer lobby id
            messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobby.getLobbyId() + "/rematch-result", result);
            lobbyService.closeLobby(lobby.getLobbyId());
            try {
                Thread.sleep(1000); // 500ms Verzögerung, damit Clients subscriben können
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Starte das neue Spiel wie bei normalem Start
            startGameRest(new StartGameRequest(
                    newLobbyCode,
                    lobby.getGameTime().name(),
                    userService.getUsernameById(lobby.getWhitePlayer()),
                    userService.getUsernameById(lobby.getBlackPlayer()),
                    lobby.isRandomColors())
            );
            logger.info("neuer lobby code: {}", newLobbyCode);
        }else{
            RematchResult result = new RematchResult(lobby.getLobbyId(), response.getResponse(), null);
            messagingTemplate.convertAndSend(TOPIC_LOBBY_PREFIX + lobby.getLobbyId() + "/rematch-result", result);
        }
    }

    @MessageMapping("/game/start")
    public GameStartResponse startGameRest(StartGameRequest request) {
        logger.info("Received start request: {}", request);
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return new GameStartResponse(false, null, null, null, null, "", null, "Lobby-ID fehlt");
        }
        Lobby lobby = lobbyService.getLobby(request.getLobbyId());
        if (lobby == null) {
            return new GameStartResponse(false, request.getLobbyId(), null, null, null, TOPIC_LOBBY_PREFIX + request.getLobbyId(), null, "Lobby nicht gefunden");
        }
        if (request.getGameTime() != null) {
            try {
                lobby.setGameTime(GameTime.valueOf(request.getGameTime()));
            } catch (IllegalArgumentException e) {
                return new GameStartResponse(false, request.getLobbyId(), request.getGameTime(), null, null, TOPIC_LOBBY_PREFIX + request.getLobbyId(), null, "Ungültige Spielzeit");
            }
        }
        lobby.setRandomColors(request.isRandomPlayers());
        if(!lobby.isRandomColors()){
            lobby.setWhitePlayer(userService.getIdByUsername(request.getWhitePlayer()).toString());
            lobby.setBlackPlayer(userService.getIdByUsername(request.getBlackPlayer()).toString());
        }
        lobbyService.startGame(lobby);
        Optional<User> whiteUserOpt = userService.getUserById(lobby.getWhitePlayer());
        Optional<User> blackUserOpt = userService.getUserById(lobby.getBlackPlayer());
        if (whiteUserOpt.isEmpty() || blackUserOpt.isEmpty()) {
            String missing = whiteUserOpt.isEmpty() ? "WhitePlayer ('" + lobby.getWhitePlayer() + "')" : "BlackPlayer ('" + lobby.getBlackPlayer() + "')";
            logger.warn("Spieler nicht in der Datenbank gefunden: {}", missing);
            return new GameStartResponse(false, request.getLobbyId(), request.getGameTime(),
                    whiteUserOpt.map(User::getUsername).orElse(null),
                    blackUserOpt.map(User::getUsername).orElse(null),
                    TOPIC_LOBBY_PREFIX + request.getLobbyId(), null,
                    missing + " konnte nicht gefunden werden");
        }
        UUID whitePlayerId = whiteUserOpt.get().getId();
        UUID blackPlayerId = blackUserOpt.get().getId();
        String whitePlayerName = whiteUserOpt.get().getUsername();
        String blackPlayerName = blackUserOpt.get().getUsername();
        GameEntity gameEntity = new GameEntity();
        gameEntity.setWhitePlayerId(whitePlayerId);
        gameEntity.setBlackPlayerId(blackPlayerId);
        gameEntity.setStartedAt(OffsetDateTime.now());
        gameEntity.setLobbyId(request.getLobbyId());
        gameRepository.save(gameEntity);
        gameManager.startGame(request.getLobbyId(), gameEntity.getId());
        messagingTemplate.convertAndSend(
                TOPIC_LOBBY_PREFIX + request.getLobbyId(),
                Map.of(
                        "type", "gameStarted",
                        "success", true,
                        "lobbyId", request.getLobbyId(),
                        "gameTime", request.getGameTime(),
                        "whitePlayer", whitePlayerName,
                        "blackPlayer", blackPlayerName,
                        "randomPlayers", request.isRandomPlayers(),
                        "board", getBoardForLobby(request.getLobbyId())
                )
        );
        return new GameStartResponse(
                true,
                request.getLobbyId(),
                request.getGameTime(),
                whitePlayerName,
                blackPlayerName,
                TOPIC_LOBBY_PREFIX + request.getLobbyId(),
                getBoardForLobby(request.getLobbyId()),
                null
        );
    }
}
