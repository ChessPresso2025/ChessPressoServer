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
import org.example.chesspressoserver.models.requests.RematchRequest;
import org.example.chesspressoserver.models.requests.ResignGameRequest;
import org.example.chesspressoserver.models.requests.StartGameRequest;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.GameHistoryService;
import org.example.chesspressoserver.service.StatsService;
import org.example.chesspressoserver.service.UserService;
import org.example.chesspressoserver.repository.GameRepository;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class GameRestController {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final UserService userService;
    private final GameHistoryService gameHistoryService;
    private final GameRepository gameRepository;
    private final StatsService statsService;

    @Autowired
    public GameRestController(GameManager gameManager, SimpMessagingTemplate messagingTemplate, LobbyService lobbyService, UserService userService, GameHistoryService gameHistoryService, GameRepository gameRepository, StatsService statsService) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.gameHistoryService = gameHistoryService;
        this.gameRepository = gameRepository;
        this.statsService = statsService;
    }

    @MessageMapping("/game/start")
    public GameStartResponse startGame(StartGameRequest request) {
        System.out.println("Received start request: " + request);
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return new GameStartResponse(false, null, null, null, null, "", null, "Lobby-ID fehlt");
        }
        Lobby lobby = lobbyService.getLobby(request.getLobbyId());
        if (lobby == null) {
            return new GameStartResponse(false, request.getLobbyId(), null, null, null, "/topic/lobby/" + request.getLobbyId(), null, "Lobby nicht gefunden");
        }
        // gameTime als Enum setzen, falls nötig
        if (request.getGameTime() != null) {
            try {
                lobby.setGameTime(GameTime.valueOf(request.getGameTime()));
            } catch (IllegalArgumentException e) {
                return new GameStartResponse(false, request.getLobbyId(), request.getGameTime(), null, null, "/topic/lobby/" + request.getLobbyId(), null, "Ungültige Spielzeit");
            }
        }
        lobby.setRandomColors(request.isRandomPlayers());

        String whitePlayer = request.getWhitePlayer();
        String blackPlayer = request.getBlackPlayer();

        if (request.isRandomPlayers()) {
            List<String> players = lobby.getPlayers();
            if (players == null || players.size() != 2) {
                return new GameStartResponse(false, request.getLobbyId(), request.getGameTime(), null, null, "/topic/lobby/" + request.getLobbyId(), null, "Es müssen genau zwei Spieler in der Lobby sein, um die Farben zufällig zuzuweisen.");
            }
            List<String> shuffled = new ArrayList<>(players);
            java.util.Collections.shuffle(shuffled);
            whitePlayer = shuffled.get(0);
            blackPlayer = shuffled.get(1);
        }
        lobby.setWhitePlayer(whitePlayer);
        lobby.setBlackPlayer(blackPlayer);
        lobby.setGameStarted(true);
        lobby.setStatus(LobbyStatus.IN_GAME);

        // Hole die User-Objekte anhand des Usernamens (falls nötig)
        Optional<User> whiteUserOpt = userService.getUserByUsername(whitePlayer);
        Optional<User> blackUserOpt = userService.getUserByUsername(blackPlayer);
        if (whiteUserOpt.isEmpty() || blackUserOpt.isEmpty()) {
            String missing = whiteUserOpt.isEmpty() ? "WhitePlayer ('" + whitePlayer + "')" : "BlackPlayer ('" + blackPlayer + "')";
            System.out.println("Spieler nicht in der Datenbank gefunden: " + missing);
            return new GameStartResponse(false, request.getLobbyId(), request.getGameTime(),
                    whiteUserOpt.isPresent() ? whiteUserOpt.get().getUsername() : null,
                    blackUserOpt.isPresent() ? blackUserOpt.get().getUsername() : null,
                    "/topic/lobby/" + request.getLobbyId(), null,
                    missing + " konnte nicht gefunden werden");
        }
        UUID whitePlayerId = whiteUserOpt.get().getId();
        UUID blackPlayerId = blackUserOpt.get().getId();
        String whitePlayerName = whiteUserOpt.get().getUsername();
        String blackPlayerName = blackUserOpt.get().getUsername();

        // GameEntity erzeugen und speichern
        GameEntity gameEntity = new GameEntity();
        gameEntity.setWhitePlayerId(whitePlayerId);
        gameEntity.setBlackPlayerId(blackPlayerId);
        gameEntity.setStartedAt(OffsetDateTime.now());
        gameEntity.setLobbyId(request.getLobbyId());
        gameRepository.save(gameEntity);
        // GameManager mit gameId starten
        gameManager.startGame(request.getLobbyId(), gameEntity.getId());
        // WebSocket-Benachrichtigung an alle Clients der Lobby
        messagingTemplate.convertAndSend(
                // Setze die LobbyId
                "/topic/lobby/" + request.getLobbyId(),
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
        // GameStartResponse zurückgeben
        return new GameStartResponse(
                true,
                request.getLobbyId(),
                request.getGameTime(),
                whitePlayerName,
                blackPlayerName,
                "/topic/lobby/" + request.getLobbyId(),
                getBoardForLobby(request.getLobbyId()),
                null
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

    @PostMapping("/rematch")
    public ResponseEntity<?> requestRematch(@RequestBody RematchRequest request) {
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby-ID fehlt");
        }
        boolean success = gameManager.rematch(request.getLobbyId());
        if (success) {
            return ResponseEntity.ok().body("Rematch gestartet für Lobby-ID: " + request.getLobbyId());
        } else {
            return ResponseEntity.badRequest().body("Ungültige Lobby-ID für Rematch");
        }
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
            game.setEndedAt(java.time.OffsetDateTime.now());
            game.setResult(result);
            gameRepository.save(game);
        }
    }

    private void handleGameEndCommon(GameEndMessage message, Lobby lobby, EndType endType, boolean callResignGame) {
        String loser;
        String winner;
        Optional<User> whiteUserOpt = userService.getUserByUsername(lobby.getWhitePlayer());
        Optional<User> blackUserOpt = userService.getUserByUsername(lobby.getBlackPlayer());
        UUID whiteId = whiteUserOpt.map(User::getId).orElse(null);
        UUID blackId = blackUserOpt.map(User::getId).orElse(null);
        String result;
        boolean success = true;
        if (callResignGame) {
            success = gameManager.resignGame(message.getLobbyId());
        }
        if (message.getPlayer() == TeamColor.WHITE) {
            loser = lobby.getWhitePlayer();
            winner = lobby.getBlackPlayer();
            result = "BLACK_WIN";
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
            loser = lobby.getBlackPlayer();
            winner = lobby.getWhitePlayer();
            result = "WHITE_WIN";
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
        updateGameEndInDatabase(message.getLobbyId(), result);
        gameManager.removeGameByLobbyId(message.getLobbyId());
        String reason = switch (endType) {
            case RESIGNATION -> "RESIGN";
            case TIMEOUT -> "TIMEOUT";
            case CHECKMATE -> "CHECKMATE";
            default -> null;
        };
        if (!callResignGame || success) {
            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + message.getLobbyId(),
                    new GameEndResponse(userService.getUsernameById(winner), userService.getUsernameById(loser), false, lobby.getLobbyId(), reason)
            );
            // lobbyService.closeLobby(message.getLobbyId()); // Entfernt, damit Rematch möglich bleibt
        } else {
            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + message.getLobbyId(),
                    Map.of(
                            "type", "gameResigned",
                            "lobbyId", message.getLobbyId(),
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
        updateGameEndInDatabase(message.getLobbyId(), "DRAW");
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + message.getLobbyId(),
                new GameEndResponse(null, null, true, lobby.getLobbyId(), "DRAW")
        );
        // gameManager.removeGameByLobbyId(message.getLobbyId()); // Entfernt, damit Rematch möglich bleibt
        // lobbyService.closeLobby(message.getLobbyId()); // Entfernt, damit Rematch möglich bleibt
        //TODO: weitere Logik für Remis
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
        System.out.println("Remisnachricht erhalten");
        // Remis-Angebot: responder == null
        if (remisMessage.getResponder() == TeamColor.NULL) {
            System.out.println("Remisangebot erkannt");
            String sender = remisMessage.getRequester() == TeamColor.WHITE ? lobby.getWhitePlayer() : lobby.getBlackPlayer();
            String receiver = remisMessage.getRequester() == TeamColor.WHITE ? lobby.getBlackPlayer() : lobby.getWhitePlayer();
            String receiver_name = userService.getUsernameById(receiver);
            messagingTemplate.convertAndSend(
                "/topic/lobby/remis/" + receiver_name,
                Map.of(
                    "type", "remis",
                    "requester", remisMessage.getRequester(),
                    "accept", false,
                    "lobbyId", remisMessage.getLobbyId()
                )
            );
            System.out.println("Remisnachricht erstellt für Spieler " + receiver_name);
        } else if (remisMessage.isAccept()) {
            // Remis wurde angenommen: Spiel als Remis beenden
            updateGameEndInDatabase(remisMessage.getLobbyId(), "DRAW");
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + remisMessage.getLobbyId(),
                new GameEndResponse(null, null, true, lobby.getLobbyId(), "DRAW")
            );
            gameManager.removeGameByLobbyId(remisMessage.getLobbyId());
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
}
