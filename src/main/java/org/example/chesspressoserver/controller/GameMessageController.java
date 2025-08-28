package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.gamelogic.GameController;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GameMessageController {
    private final GameController gameController;
    private final SimpMessagingTemplate messagingTemplate;

    public GameMessageController(GameController gameController, SimpMessagingTemplate messagingTemplate) {
        this.gameController = gameController;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/request")
    @SendTo("/topic/game/possibleMoves")
    public List<Position> handleRequest(@Payload PositionRequest request) {
        Position position = new Position(request.getPosition());
        return gameController.getMovesForRequest(position);
    }

    @MessageMapping("/game/move")
    public void handleMove(@Payload MoveRequest moveRequest) {
        // Sende zuerst das aktive Team
        messagingTemplate.convertAndSend("/topic/game/activeTeam", gameController.getAktiveTeam());

        Position start = new Position(moveRequest.getFrom());
        Position end = new Position(moveRequest.getTo());
        PieceType promotionType = moveRequest.getPromotionType() != null ?
            PieceType.valueOf(moveRequest.getPromotionType()) : null;

        Move move = gameController.applyMove(start, end, promotionType);

        // Hole das aktuelle Board
        Board board = gameController.getBoard();
        Map<String, PieceInfo> boardMap = getCurrentBoard();

        // Überprüfe auf Schach
        Position checkedKingPosition = null;
        Position kingPos = board.getKingPosition(gameController.getAktiveTeam());
        if (kingPos != null && gameController.isSquareAttackedBy(
                gameController.getAktiveTeam() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE,
                kingPos)) {
            checkedKingPosition = kingPos;
        }

        // Sende Board-Update
        messagingTemplate.convertAndSend("/topic/game/board", boardMap);

        // Sende Move-Info mit Schach-Position
        MoveResponse moveResponse = new MoveResponse(move, checkedKingPosition);
        messagingTemplate.convertAndSend("/topic/game/move", moveResponse);
    }

    @MessageMapping("/game/start")
    public void startGame() {
        // Sende das initiale Board
        messagingTemplate.convertAndSend("/topic/game/board", getCurrentBoard());

        // Sende das aktive Team (zu Beginn immer Weiß)
        messagingTemplate.convertAndSend("/topic/game/activeTeam", gameController.getAktiveTeam());
    }

    public Map<String, PieceInfo> getCurrentBoard() {
        Board board = gameController.getBoard();
        Map<String, PieceInfo> boardMap = new HashMap<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Position pos = new Position(x, y);
                ChessPiece piece = board.getPiece(y, x);
                if (piece != null) {
                    boardMap.put(pos.toString(), new PieceInfo(piece.getType(), piece.getColour()));
                }else  {
                    boardMap.put(pos.toString(), new PieceInfo(null, null));
                }
            }
        }
        return boardMap;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionRequest {
        private String position;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveRequest {
        private String from;
        private String to;
        private String promotionType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveResponse {
        private Move move;
        private Position checkedKingPosition; // null wenn kein Schach
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceInfo {
        private PieceType type;
        private TeamColor teamColor;
    }
}
