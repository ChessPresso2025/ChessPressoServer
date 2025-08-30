package org.example.chesspressoserver.controller;

import lombok.*;
import org.example.chesspressoserver.gamelogic.GameController;
import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class GameMessageController {
    @Getter
    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;

    public GameMessageController(GameManager gameManager, SimpMessagingTemplate messagingTemplate) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/position-request")
    public void handleRequest(@Payload PositionRequest request) {
        String lobbyId = request.lobbyId;
        Position  position = new Position(request.position);
        GameController gameController = gameManager.getGameByLobby(lobbyId);
        if (gameController == null) return;
        List<String> moves = gameController.getMovesForRequestAsString(position);
        messagingTemplate.convertAndSend(
                "/topic/game/" + lobbyId + "/possible-moves",
                Map.of("type", "possible-moves", "possibleMoves", moves)
        );
    }

    @MessageMapping("/game/move")
    public void handleMove(@Payload MoveRequest moveRequest) {
        String lobbyId = moveRequest.lobbyId;
            Position start = new Position(moveRequest.getFrom());
            Position end = new Position(moveRequest.getTo());
            GameController gameController = gameManager.getGameByLobby(lobbyId);
            if (gameController == null) return;
            PieceType promotedPiece = moveRequest.getPromotedPiece();
            System.out.println("Zu umwandelnde Figur: " + promotedPiece);
            //check pawn promotion before applyMove()
            ChessPiece moving = gameController.getBoard().getPiece(start.getY(), start.getX());
            boolean isPromotion = checkPromotion(end, moving);
            if(isPromotion && (promotedPiece == null || promotedPiece == PieceType.NULL)) {
                messagingTemplate.convertAndSend("/topic/game/" + moveRequest.lobbyId + "/move/promotion", new PromotionRequest(moveRequest.to, moveRequest.from, moveRequest.teamColor));
                return;
            }
            Move move = gameController.applyMove(start, end, promotedPiece);
            Board board = gameController.getBoard();
            Map<String, PieceInfo> boardMap = getCurrentBoard(gameController);

            Position checkedKingPosition = null;
            TeamColor opposingTeam = gameController.getAktiveTeam() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
            Position kingPos = board.getKingPosition(gameController.getAktiveTeam());

            if (kingPos != null && gameController.isSquareAttackedBy(opposingTeam, kingPos)) {
                checkedKingPosition = kingPos;
            }

            String isCheck = checkedKingPosition != null ? checkedKingPosition.getPos() : "";

            SendMove sendMove = new SendMove(move);

            messagingTemplate.convertAndSend(
                "/topic/game/" + moveRequest.lobbyId + "/move",
                    new MoveResponse(boardMap, isCheck, gameController.getAktiveTeam(), moveRequest.lobbyId, sendMove)
            );
    }

    public Map<String, PieceInfo> getCurrentBoard(GameController gameController) {
        Board board = gameController.getBoard();
        Map<String, PieceInfo> boardMap = new HashMap<>();
        for(int x = 0; x < 8; x++){
            for (int y = 0; y < 8; y++) {
                Position pos = new Position(x, y);
                ChessPiece piece = board.getPiece(y, x);
                if (piece != null) {
                    boardMap.put(pos.toString(), new PieceInfo(piece.getType(), piece.getColour()));
                }else{
                    boardMap.put(pos.toString(), new PieceInfo(PieceType.NULL, TeamColor.NULL));
                }
            }
        }
        return boardMap;
    }



    private boolean checkPromotion(Position end, ChessPiece moving) {
        boolean isPromotion = false;
        if (moving.getType() == PieceType.PAWN) {
            if ((moving.getColour() == TeamColor.WHITE && end.getY() == 7) ||
                    (moving.getColour() == TeamColor.BLACK && end.getY() == 0)) {
                isPromotion = true;
            }
        }
        return isPromotion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionRequest {
        private String lobbyId;
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
        private TeamColor teamColor;
        private PieceType promotedPiece;
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
        private SendMove move;
    }

    @Data
    public static class PromotionRequest {
        private final String type = "promotion";
        private final String position;
        private final String from;
        private final TeamColor activeTeam;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceInfo {
        private PieceType type;
        private TeamColor color;
    }
    @Data
    public static class SendMove{
        private final String start;
        private final String end;
        private final PieceType piece;
        @Setter
        private SpezialMove spezialMove;
        @Setter
        private SendCapturedInfo captured;
        public SendMove(Move move) {
            this.captured = new SendCapturedInfo(move.getCaptured());
            this.spezialMove = move.getSpezialMove();
            this.piece = move.getPiece();
            this.end = move.getEnd().toString();
            this.start = move.getStart().toString();
        }
    }

    @Data
    public static class SendCapturedInfo{
        private PieceType type = null;
        private TeamColor colour = null;
        private String position = null;

        public SendCapturedInfo(CapturedInfo capturedInfo) {
            if(capturedInfo == null){
                return;
            }
            this.type = capturedInfo.getType();
            this.colour = capturedInfo.getColour();
            this.position = capturedInfo.getPosition().toString();
        }
    }
}
