package org.example.chesspressoserver.controller;

import lombok.*;
import org.example.chesspressoserver.gamelogic.GameController;
import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.*;
import org.example.chesspressoserver.repository.MoveRepository;
import org.example.chesspressoserver.repository.GameRepository;
import org.example.chesspressoserver.models.MoveEntity;
import org.example.chesspressoserver.models.GameEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

@Controller
public class GameMessageController {
    @Getter
    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final MoveRepository moveRepository;
    private final GameRepository gameRepository;

    public GameMessageController(GameManager gameManager, SimpMessagingTemplate messagingTemplate, MoveRepository moveRepository, GameRepository gameRepository) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
        this.moveRepository = moveRepository;
        this.gameRepository = gameRepository;
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

        // Check pawn promotion before applyMove()
        ChessPiece moving = gameController.getBoard().getPiece(start.getY(), start.getX());
        boolean isPromotion = checkPromotion(end, moving);
        if(isPromotion && (promotedPiece == null || promotedPiece == PieceType.NULL)) {
            messagingTemplate.convertAndSend("/topic/game/" + moveRequest.lobbyId + "/move/promotion",
                new PromotionRequest(moveRequest.to, moveRequest.from, moveRequest.teamColor));
            return;
        }

        // Führe den Zug aus
        Move move = gameController.applyMove(start, end, promotedPiece);
        Map<String, PieceInfo> boardMap = getCurrentBoard(gameController);

        // Prüfe auf Schach und Schachmatt
        Position checkedKingPosition = null;
        List<String> checkMatePositions = new ArrayList<>();

        // Hole den aktiven König (der, der gerade am Zug ist)
        Position kingPos = gameController.getBoard().getKingPosition(gameController.getAktiveTeam());

        if (kingPos != null) {
            // Prüfe ob der König im Schach steht (nutze die currentAttackers Liste)
            if (!gameController.getCurrentAttackers().isEmpty()) {
                checkedKingPosition = kingPos;

                // Wenn der König im Schach steht, prüfe auf Schachmatt
                if (gameController.isCheckMate(kingPos, gameController.getAktiveTeam())) {
                    // Nutze die aktuelle Liste der Angreifer für die Response
                    checkMatePositions = gameController.getCurrentAttackers().stream()
                        .map(Position::getPos)
                        .collect(Collectors.toList());
                }
            }
        }

        String isCheck = checkedKingPosition != null ? checkedKingPosition.getPos() : "";
        SendMove sendMove = new SendMove(move);

        if(!gameController.isStalemate(gameController.getAktiveTeam())){
        // Sende die Response
        messagingTemplate.convertAndSend(
            "/topic/game/" + moveRequest.lobbyId + "/move",
            new MoveResponse(boardMap, isCheck, gameController.getAktiveTeam(),
                moveRequest.lobbyId, sendMove, checkMatePositions, false)
        );}
        else{
            // Sende die Response bei Patt
            messagingTemplate.convertAndSend(
                "/topic/game/" + moveRequest.lobbyId + "/move",
        new MoveResponse(boardMap, isCheck, gameController.getAktiveTeam(),
            moveRequest.lobbyId, sendMove, checkMatePositions, true)
        );
        }

        // Nach applyMove: Zug in DB speichern
        java.util.UUID gameId = gameController.getGameId();
        GameEntity gameEntity = gameRepository.findById(gameId).orElse(null);
        if (gameEntity != null) {
            MoveEntity moveEntity = new MoveEntity();
            moveEntity.setGame(gameEntity);
            int moveNumber = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId).size() + 1;
            moveEntity.setMoveNumber(moveNumber);
            String pieceAscii = getAsciiForPiece(moving.getType(), moving.getColour());
            moveEntity.setMoveNotation(
                pieceAscii + ": " + move.getStart().getPos().toLowerCase() + " -> " + move.getEnd().getPos().toLowerCase()
            );
            moveEntity.setCreatedAt(OffsetDateTime.now());
            moveRepository.save(moveEntity);
        }
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

    // Hilfsmethode für ASCII-Symbole
    private String getAsciiForPiece(PieceType type, TeamColor color) {
        if (color == TeamColor.WHITE) {
            return switch (type) {
                case PAWN -> "♙";
                case KNIGHT -> "♘";
                case BISHOP -> "♗";
                case ROOK -> "♖";
                case QUEEN -> "♕";
                case KING -> "♔";
                default -> " ";
            };
        } else if (color == TeamColor.BLACK) {
            return switch (type) {
                case PAWN -> "♟";
                case KNIGHT -> "♞";
                case BISHOP -> "♝";
                case ROOK -> "♜";
                case QUEEN -> "♛";
                case KING -> "♚";
                default -> " ";
            };
        }
        return " ";
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
        private boolean isStalemate = false;
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
