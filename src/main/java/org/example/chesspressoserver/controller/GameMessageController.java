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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GameMessageController {
    @Getter
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
            Position start = new Position(moveRequest.getFrom());
            Position end = new Position(moveRequest.getTo());

            PieceType promotionType = null;
            if (moveRequest.getPromotionType() != null && !moveRequest.getPromotionType().isEmpty()) {
                promotionType = PieceType.valueOf(moveRequest.getPromotionType());
            }

            Move move = gameController.applyMove(start, end, promotionType);
            Board board = gameController.getBoard();
            Map<String, PieceInfo> boardMap = getCurrentBoard();

            Position checkedKingPosition = null;
            TeamColor opposingTeam = gameController.getAktiveTeam() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            Position kingPos = board.getKingPosition(opposingTeam);

            if (kingPos != null && gameController.isSquareAttackedBy(gameController.getAktiveTeam(), kingPos)) {
                checkedKingPosition = kingPos;
            }

            String isCheck = checkedKingPosition != null ? checkedKingPosition.getPos() : "";

            messagingTemplate.convertAndSend(
                "/topic/game/" + moveRequest.lobbyId + "/move",
                    new MoveResponse(boardMap, isCheck, gameController.getAktiveTeam(), moveRequest.lobbyId, move)
            );
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
        private String lobbyId;
        private String from;
        private String to;
        private String promotionType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoveResponse {
        private final String type = "move";
        private Map <String, PieceInfo> board;
        private String isCheck;
        private TeamColor activeTeam;
        private String lobbyId;
        private Move move;
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
