package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.dto.GameStartResponse;
import org.example.chesspressoserver.dto.PieceInfo;
import org.example.chesspressoserver.gamelogic.GameController;
import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.PieceType;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;
import org.example.chesspressoserver.models.requests.RematchRequest;
import org.example.chesspressoserver.models.requests.ResignGameRequest;
import org.example.chesspressoserver.models.requests.StartGameRequest;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.models.Lobby;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.models.LobbyStatus;
import org.example.chesspressoserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GameRestController {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final UserService userService;

    @Autowired
    public GameRestController(GameManager gameManager, SimpMessagingTemplate messagingTemplate, LobbyService lobbyService, UserService userService) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    @MessageMapping("/game/start")
    @SendTo("/topic/game/start/response")
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

        String whitePlayerName = userService.getUsernameById(whitePlayer);
        String blackPlayerName = userService.getUsernameById(blackPlayer);

        gameManager.startGame(request.getLobbyId());
        // WebSocket-Benachrichtigung an alle Clients der Lobby
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + request.getLobbyId(),
            Map.of(
                "type", "gameStarted",
                "success", true,
                "lobbyId", request.getLobbyId() ,
                "gameTime", request.getGameTime(),
                "whitePlayer", whitePlayerName ,
                "blackPlayer", blackPlayerName ,
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
                ChessPiece piece = board.getPiece(y,x);
                if (piece != null) {
                    boardMap.put(pos.toString(), new PieceInfo(piece.getType(), piece.getColour()));
                } else {
                    boardMap.put(pos.toString(), new PieceInfo(PieceType.NULL, TeamColor.NULL));
                }
            }
        }
        return boardMap;
    }

    @PostMapping("/resign")
    public ResponseEntity<?> resignGame(@RequestBody ResignGameRequest request) {
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby-ID fehlt");
        }
        boolean success = gameManager.resignGame(request.getLobbyId());
        if (success) {
            return ResponseEntity.ok().body("Spiel für Lobby aufgegeben");
        } else {
            return ResponseEntity.badRequest().body("Ungültige Lobby-ID");
        }
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
}
