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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        //check pawn promotion before applyMove()
        ChessPiece moving = gameController.getBoard().getPiece(start.getY(), start.getX());
        boolean isPromotion = checkPromotion(end, moving);
        if(isPromotion && (promotedPiece == null || promotedPiece == PieceType.NULL)) {
            messagingTemplate.convertAndSend("/topic/game/" + moveRequest.lobbyId + "/move/promotion",
                new PromotionRequest(moveRequest.to, moveRequest.from, moveRequest.teamColor));
            return;
        }

        Move move = gameController.applyMove(start, end, promotedPiece);
        Board board = gameController.getBoard();
        Map<String, PieceInfo> boardMap = getCurrentBoard(gameController);

        Position checkedKingPosition = null;
        TeamColor opposingTeam = gameController.getAktiveTeam() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPos = board.getKingPosition(gameController.getAktiveTeam());

        List<String> checkMatePositions = new ArrayList<>();
        if (kingPos != null && gameController.isSquareAttackedBy(opposingTeam, kingPos)) {
            checkedKingPosition = kingPos;
            // Wenn der König im Schach steht, prüfe auf Schachmatt
            if (gameController.isCheckMate(kingPos, gameController.getAktiveTeam())) {
                // Hole die angreifenden Positionen für die Response
                List<Position> attackers = gameController.getAttackingPositions(kingPos, opposingTeam);
                checkMatePositions = attackers.stream()
                    .map(Position::getPos)
                    .collect(Collectors.toList());
            }
        }

        String isCheck = checkedKingPosition != null ? checkedKingPosition.getPos() : "";
        SendMove sendMove = new SendMove(move);

        messagingTemplate.convertAndSend(
            "/topic/game/" + moveRequest.lobbyId + "/move",
            new MoveResponse(boardMap, isCheck, gameController.getAktiveTeam(),
                moveRequest.lobbyId, sendMove, checkMatePositions)
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

    private List<Position> getAttackingPositions(GameController gameController, Position kingPos, TeamColor attackingTeam) {
        List<Position> attackingPositions = new ArrayList<>();
        Board board = gameController.getBoard();

        for(int x = 0; x < 8; x++) {
            for(int y = 0; y < 8; y++) {
                Position pos = new Position(x, y);
                ChessPiece piece = board.getPiece(y, x);
                if(piece != null && piece.getColour() == attackingTeam) {
                    List<String> possibleMoves = gameController.getMovesForRequestAsString(pos);
                    if(possibleMoves.contains(kingPos.getPos())) {
                        attackingPositions.add(pos);
                    }
                }
            }
        }
        return attackingPositions;
    }

    private boolean isCheckMate(GameController gameController, Position kingPos, TeamColor defendingTeam) {
        // 1. Prüfe ob der König sich bewegen kann
        List<String> kingMoves = gameController.getMovesForRequestAsString(kingPos);
        if(!kingMoves.isEmpty()) {
            return false;
        }

        // 2. Hole alle angreifenden Positionen
        TeamColor attackingTeam = defendingTeam == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE;
        List<Position> attackers = getAttackingPositions(gameController, kingPos, attackingTeam);

        // Wenn mehr als ein Angreifer und der König sich nicht bewegen kann, ist es Schachmatt
        if(attackers.size() > 1) {
            return true;
        }

        // 3. Bei einem Angreifer: Prüfe ob eine verteidigende Figur den Angreifer schlagen oder blocken kann
        Position attacker = attackers.get(0);
        Board board = gameController.getBoard();

        // Prüfe alle verteidigenden Figuren
        for(int x = 0; x < 8; x++) {
            for(int y = 0; y < 8; y++) {
                Position defenderPos = new Position(x, y);
                ChessPiece piece = board.getPiece(y, x);
                if(piece != null && piece.getColour() == defendingTeam && !defenderPos.equals(kingPos)) {
                    List<String> moves = gameController.getMovesForRequestAsString(defenderPos);

                    // Kann der Angreifer geschlagen werden?
                    if(moves.contains(attacker.getPos())) {
                        return false;
                    }

                    // Kann eine Figur zwischen König und Angreifer ziehen?
                    for(String move : moves) {
                        Position blockingPos = new Position(move);
                        if(isPositionBetween(blockingPos, kingPos, attacker)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean isPositionBetween(Position pos, Position start, Position end) {
        // Vereinfachte Implementierung für gerade Linien (horizontal, vertikal, diagonal)
        int dx = Integer.compare(end.getX() - start.getX(), 0);
        int dy = Integer.compare(end.getY() - start.getY(), 0);

        Position current = new Position(start.getX() + dx, start.getY() + dy);
        while(!current.equals(end)) {
            if(current.equals(pos)) {
                return true;
            }
            current = new Position(current.getX() + dx, current.getY() + dy);
        }
        return false;
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
        private List<String> checkMatePositions;
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
