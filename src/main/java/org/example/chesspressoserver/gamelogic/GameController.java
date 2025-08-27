package org.example.chesspressoserver.gamelogic;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.gamelogic.modles.CastlingRights;
import org.example.chesspressoserver.models.gamemodels.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameController {

    @Setter
    private Board board;

    @Setter
    private TeamColor aktiveTeam;

    @Setter
    private CastlingRights castlingRights = new CastlingRights();

    @Setter
    private Move lastMove;

    // =====================================================================
    // 1) REQUEST: alle LEGALEN Züge für die Figur an startPos
    // =====================================================================

    // Methode für die Requesteingabe, gibt alle möglichen Moves an den Client
    public List<Position> getMovesForRequest(final Position startPos) {
        ChessPiece piece = board.getPiece(startPos.getY(), startPos.getX());
        if (piece == null || piece.getColour() != aktiveTeam) return List.of();

        TeamColor me = piece.getColour();
        TeamColor enemy = (me == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // 1) Pseudo-Moves
        List<Position> moves = new ArrayList<>(piece.getMove().getPossibleMoves(startPos, board));

        // 2) Sonderfälle
        if (piece.getType() == PieceType.KING) {
            moves = getKingMovesWithCastling(startPos, moves);
        } else {
            if (piece.getType() == PieceType.PAWN) {
                addEnPassantMoves(startPos, moves);
            }
            // Pin-/Block-Schnittmenge bei geometrischer Verbindung zum eigenen König
            if (checkKingConnection(startPos)) {
                List<Position> corridor = getKingConnectionPostion(startPos); // exkl. start, inkl. Angreifer
                if (!corridor.isEmpty()) moves.retainAll(corridor);
            }
        }

        // 3) King-Safety (Simulation)
        Position myKing = board.getKingPosition(me);
        moves.removeIf(dst -> leavesKingInCheck(startPos, dst, myKing, enemy));

        return moves;
    }

    // =====================================================================
    // 2) ZUG AUSFÜHREN: wendet einen legalen Zug an und gibt Info zurück
    // =====================================================================

    // Methode setzt den Move, welcher vom Client kommt um und sendet die Info an den Client zurück
    public Move applyMove(final Position start, final Position end, PieceType promotionChoice) {
        ChessPiece moving = board.getPiece(start.getY(), start.getX());
        if (moving == null || moving.getColour() != aktiveTeam) {
            throw new IllegalStateException("No active-team piece at start.");
        }

        Move result = new Move(start, end, moving.getType());

        // --- evtl. Normal-Capture am Ziel bestimmen
        ChessPiece targetAtEnd = board.getPiece(end.getY(), end.getX());
        if (targetAtEnd != null && targetAtEnd.getColour() != moving.getColour()) {
            result.setCaptured(new CapturedInfo(targetAtEnd.getType(), targetAtEnd.getColour(), end));
        }

        boolean isPawn   = (moving.getType() == PieceType.PAWN);
        boolean isCastle = (moving.getType() == PieceType.KING) && (Math.abs(end.getX() - start.getX()) == 2);
        boolean isEP     = false;

        // --- En Passant?
        Position epVictimPos = null;
        if (isPawn && lastMove != null && lastMove.getPiece() == PieceType.PAWN && targetAtEnd == null) {
            TeamColor me = moving.getColour();
            TeamColor enemy = (me == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

            Position from = lastMove.getStart();
            Position to   = lastMove.getEnd();
            int enemyTwoFromY = (enemy == TeamColor.WHITE) ? 1 : 6;
            int enemyTwoToY   = (enemy == TeamColor.WHITE) ? 3 : 4;

            boolean wasDoublePush = (from.getY() == enemyTwoFromY && to.getY() == enemyTwoToY && from.getX() == to.getX());
            boolean sideBySide    = (start.getY() == to.getY() && Math.abs(start.getX() - to.getX()) == 1);
            int expectedEPy       = (me == TeamColor.WHITE) ? (to.getY() + 1) : (to.getY() - 1);

            if (wasDoublePush && sideBySide && end.getX() == to.getX() && end.getY() == expectedEPy) {
                isEP = true;
                epVictimPos = to;
                ChessPiece epVictim = board.getPiece(epVictimPos.getY(), epVictimPos.getX());
                if (epVictim != null) {
                    result.setCaptured(new CapturedInfo(epVictim.getType(), epVictim.getColour(), epVictimPos));
                }
                result.setSpezialMove(SpezialMove.EnPassnt);
            }
        }

        // --- Promotion?
        boolean isPromotion = false;
        if (isPawn) {
            if ((moving.getColour() == TeamColor.WHITE && end.getY() == 7) ||
                    (moving.getColour() == TeamColor.BLACK && end.getY() == 0)) {
                isPromotion = true;
                result.setSpezialMove(SpezialMove.PawnPromotion);
            }
        }

        if (isCastle) {
            result.setSpezialMove(SpezialMove.Castling);
        }

        // --- Board mutieren ---
        board.removePiece(start.getY(), start.getX());                 // 1) Start leeren
        if (isEP && epVictimPos != null) {                             // 2) EP-Opfer entfernen
            board.removePiece(epVictimPos.getY(), epVictimPos.getX());
        }
        board.setPiece(end.getY(), end.getX(), moving);                // 3) Figur setzen

        // 4) Rochade: Turm mitziehen + Rechte
        if (isCastle) {
            int y = start.getY();
            if (end.getX() == 6) { // kurze
                ChessPiece rook = board.getPiece(y, 7);
                board.removePiece(y, 7);
                board.setPiece(y, 5, rook);
            } else if (end.getX() == 2) { // lange
                ChessPiece rook = board.getPiece(y, 0);
                board.removePiece(y, 0);
                board.setPiece(y, 3, rook);
            }
            if (aktiveTeam == TeamColor.WHITE) {
                castlingRights.setWhiteKingSide(false);
                castlingRights.setWhiteQueenSide(false);
            } else {
                castlingRights.setBlackKingSide(false);
                castlingRights.setBlackQueenSide(false);
            }
        }

        // 5) CastlingRights anhand Bewegung/Schläge updaten
        updateCastlingRightsOnMove(start, end, moving, result.getCaptured());

        // 6) Promotion-Figur ersetzen (falls Choice null → Default QUEEN)
        if (isPromotion) {
            if (promotionChoice == null) promotionChoice = PieceType.QUEEN;
            ChessPiece promoted = new ChessPiece(promotionChoice, moving.getColour());
            board.setPiece(end.getY(), end.getX(), promoted);
        }

        // 7) lastMove setzen (inkl. SpezialMove/Captured)
        lastMove = new Move(start, end, moving.getType(), result.getSpezialMove());
        lastMove.setCaptured(result.getCaptured());

        // 8) Zugfarbe wechseln
        aktiveTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        return result;
    }

    // =====================================================================
    // Helpers: König, EP, King-Safety, Attacks, Geometrie, CastlingRights
    // =====================================================================

    // Filtert King-Pseudo-Moves (keine angegriffenen Felder) + ergänzt Rochaden.
    private List<Position> getKingMovesWithCastling(Position start, List<Position> pseudo) {
        List<Position> legal = new ArrayList<>();
        TeamColor me = aktiveTeam;
        TeamColor enemy = (me == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // Normale King-Moves: Ziel darf nicht angegriffen sein
        for (Position p : pseudo) {
            if (!isSquareAttackedBy(enemy, p)) legal.add(p);
        }

        // Rochade (Startfeld darf nicht angegriffen sein)
        if (!isSquareAttackedBy(enemy, start)) {
            int y = start.getY();
            int x = start.getX();

            // kurze: Ziel (6,y); leer: (5,y),(6,y); Rook (7,y)
            boolean canK = (me == TeamColor.WHITE) ? castlingRights.isWhiteKingSide() : castlingRights.isBlackKingSide();
            if (canK
                    && board.checkEmpty(y, 5) && board.checkEmpty(y, 6)
                    && hasClearLine(new Position(x, y), new Position(7, y))) {
                ChessPiece rook = board.getPiece(y, 7);
                if (rook != null && rook.getType() == PieceType.ROOK && rook.getColour() == me) {
                    if (!isSquareAttackedBy(enemy, new Position(5, y))
                            && !isSquareAttackedBy(enemy, new Position(6, y))) {
                        legal.add(new Position(6, y));
                    }
                }
            }

            // lange: Ziel (2,y); leer: (1,y),(2,y),(3,y); Rook (0,y)
            boolean canQ = (me == TeamColor.WHITE) ? castlingRights.isWhiteQueenSide() : castlingRights.isBlackQueenSide();
            if (canQ
                    && board.checkEmpty(y, 1) && board.checkEmpty(y, 2) && board.checkEmpty(y, 3)
                    && hasClearLine(new Position(x, y), new Position(0, y))) {
                ChessPiece rook = board.getPiece(y, 0);
                if (rook != null && rook.getType() == PieceType.ROOK && rook.getColour() == me) {
                    if (!isSquareAttackedBy(enemy, new Position(3, y))
                            && !isSquareAttackedBy(enemy, new Position(2, y))) {
                        legal.add(new Position(2, y));
                    }
                }
            }
        }
        return legal;
    }

    // Ergänzt En-Passant-Ziel (falls erlaubt) zu den Pseudo-Bauernzügen.
    private void addEnPassantMoves(Position start, List<Position> outMoves) {
        if (lastMove == null) return;

        ChessPiece self = board.getPiece(start.getY(), start.getX());
        if (self == null || self.getType() != PieceType.PAWN) return;

        TeamColor me = self.getColour();
        TeamColor enemy = (me == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        if (lastMove.getPiece() != PieceType.PAWN) return;

        Position from = lastMove.getStart();
        Position to   = lastMove.getEnd();

        ChessPiece enemyPawnAtTo = board.getPiece(to.getY(), to.getX());
        if (enemyPawnAtTo == null || enemyPawnAtTo.getType() != PieceType.PAWN || enemyPawnAtTo.getColour() != enemy) {
            return;
        }

        int enemyTwoFromY = (enemy == TeamColor.WHITE) ? 1 : 6;
        int enemyTwoToY   = (enemy == TeamColor.WHITE) ? 3 : 4;

        if (from.getY() != enemyTwoFromY || to.getY() != enemyTwoToY || from.getX() != to.getX()) {
            return; // kein gegnerischer Doppelzug
        }

        // eigener Bauer muss seitlich (x±1) neben dem Ziel stehen (gleiche Reihe)
        if (start.getY() != to.getY()) return;
        if (Math.abs(start.getX() - to.getX()) != 1) return;

        // EP-Ziel ist das übersprungene Feld
        int epTargetY = (me == TeamColor.WHITE) ? (to.getY() + 1) : (to.getY() - 1);
        int epTargetX = to.getX();

        if (epTargetX < 0 || epTargetX > 7 || epTargetY < 0 || epTargetY > 7) return;
        if (!board.checkEmpty(epTargetY, epTargetX)) return;

        outMoves.add(new Position(epTargetX, epTargetY));
    }

    // Simuliert (start→dst) und prüft, ob danach der eigene König im Schach ist. EP wird korrekt simuliert.
    private boolean leavesKingInCheck(Position start, Position dst, Position myKing, TeamColor enemy) {
        ChessPiece moving = board.getPiece(start.getY(), start.getX());
        ChessPiece captured = board.getPiece(dst.getY(), dst.getX());

        // EP-Erkennung (dst ist EP-Zielfeld)
        boolean isEP = false;
        ChessPiece epCaptured = null;
        Position epVictimPos = null;
        if (moving != null && moving.getType() == PieceType.PAWN && lastMove != null && lastMove.getPiece() == PieceType.PAWN) {
            Position to = lastMove.getEnd();
            int expectedEPy = (moving.getColour() == TeamColor.WHITE) ? (to.getY() + 1) : (to.getY() - 1);
            if (dst.getX() == to.getX() && dst.getY() == expectedEPy
                    && start.getY() == to.getY() && Math.abs(start.getX() - to.getX()) == 1) {
                isEP = true;
                epVictimPos = to;
            }
        }

        // do move
        board.removePiece(start.getY(), start.getX());
        if (isEP && epVictimPos != null) {
            epCaptured = board.getPiece(epVictimPos.getY(), epVictimPos.getX());
            board.removePiece(epVictimPos.getY(), epVictimPos.getX());
        }
        board.setPiece(dst.getY(), dst.getX(), moving);

        Position kingPos = (moving != null && moving.getType() == PieceType.KING) ? dst : myKing;
        boolean inCheck = isSquareAttackedBy(enemy, kingPos);

        // unmove
        board.setPiece(start.getY(), start.getX(), moving);
        board.setPiece(dst.getY(), dst.getX(), captured);
        if (isEP && epVictimPos != null) {
            board.setPiece(epVictimPos.getY(), epVictimPos.getX(), epCaptured);
        }

        return inCheck;
    }

    // true, wenn 'sq' von 'attacker' angegriffen wird (Bauern: NUR Diagonalen).
    private boolean isSquareAttackedBy(TeamColor attacker, Position sq) {
        int sx = sq.getX(), sy = sq.getY();

        // Pawn (diagonals only)
        int pDir = (attacker == TeamColor.WHITE) ? -1 : +1; // aus Sicht des Angreifers
        int py = sy + pDir;
        if (py >= 0 && py < 8) {
            if (sx - 1 >= 0) {
                ChessPiece p = board.getPiece(py, sx - 1);
                if (p != null && p.getColour() == attacker && p.getType() == PieceType.PAWN) return true;
            }
            if (sx + 1 < 8) {
                ChessPiece p = board.getPiece(py, sx + 1);
                if (p != null && p.getColour() == attacker && p.getType() == PieceType.PAWN) return true;
            }
        }

        // Knight
        int[][] kJumps = {{2,1},{2,-1},{1,2},{-1,2},{-2,1},{-2,-1},{1,-2},{-1,-2}};
        for (int[] d : kJumps) {
            int x = sx + d[0], y = sy + d[1];
            if (x<0||x>7||y<0||y>7) continue;
            ChessPiece p = board.getPiece(y, x);
            if (p != null && p.getColour() == attacker && p.getType() == PieceType.KNIGHT) return true;
        }

        // King (adjacent)
        int[][] nbr = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] d : nbr) {
            int x = sx + d[0], y = sy + d[1];
            if (x<0||x>7||y<0||y>7) continue;
            ChessPiece p = board.getPiece(y, x);
            if (p != null && p.getColour() == attacker && p.getType() == PieceType.KING) return true;
        }

        // Sliders (Rook/Bishop/Queen)
        int[][] rays = {
                { 1,0},{-1,0},{0,1},{0,-1},   // orthogonal → R/Q
                { 1,1},{ 1,-1},{-1,1},{-1,-1} // diagonal  → B/Q
        };
        for (int[] d : rays) {
            int x = sx + d[0], y = sy + d[1];
            boolean ortho = (d[0] == 0 || d[1] == 0);
            while (x>=0 && x<8 && y>=0 && y<8) {
                if (!board.checkEmpty(y, x)) {
                    ChessPiece p = board.getPiece(y, x);
                    if (p != null && p.getColour() == attacker) {
                        if (ortho && (p.getType() == PieceType.ROOK || p.getType() == PieceType.QUEEN)) return true;
                        if (!ortho && (p.getType() == PieceType.BISHOP || p.getType() == PieceType.QUEEN)) return true;
                    }
                    break; // erste Figur blockiert
                }
                x += d[0]; y += d[1];
            }
        }
        return false;
    }

    // Prüft reine Geometrie (Linie/Diagonale) ohne Blocker (Zielfeld egal).
    private boolean hasClearLine(Position from, Position to) {
        if (from.getX() == to.getX() && from.getY() == to.getY()) return false;

        boolean aligned =
                (from.getX() == to.getX()) ||
                        (from.getY() == to.getY()) ||
                        (Math.abs(to.getX() - from.getX()) == Math.abs(to.getY() - from.getY()));
        if (!aligned) return false;

        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());

        int x = from.getX() + dx;
        int y = from.getY() + dy;
        while (x != to.getX() || y != to.getY()) {
            if (!board.checkEmpty(y, x)) return false;
            x += dx; y += dy;
        }
        return true;
    }

    // Gibt an, ob die Figur bei 'pos' geometrisch mit dem EIGENEN König verbunden ist (Linie/Diag ohne Blocker).
    private boolean checkKingConnection(Position pos) {
        Position king = board.getKingPosition(aktiveTeam);
        return king != null && hasClearLine(pos, king);
    }

    //Überprüft, ob durch das Bewegen der Figur ein Schach entstehen könnte
    private Position checkStateAktiveTeam(Position startPos, ChessPiece startPiece) {
        TeamColor enemyTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPosition = board.getKingPosition(aktiveTeam);

        if (kingPosition != null && kingPosition.equals(startPos)) return null;

        board.removePiece(startPos.getY(), startPos.getX()); // temporär entfernen

        Position result = null;
        outer:
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board.getPiece(row, col);
                if (piece == null || piece.getColour() != enemyTeam) continue;

                switch (piece.getType()) {
                    case QUEEN, ROOK, BISHOP -> {
                        Position pos = new Position(col, row);
                        List<Position> moves = piece.getMove().getPossibleMoves(pos, board);
                        if (kingPosition != null && moves.contains(kingPosition)) {
                            result = pos;
                            break outer;
                        }
                    }
                    default -> {}
                }
            }
        }
        board.setPiece(startPos.getY(), startPos.getX(), startPiece); // zurücksetzen
        return result;
    }

    // Felder zwischen a und b (exkl. Endpunkte) – nur bei Kollinearität (Linie/Diag).
    private List<Position> squaresBetweenExclusive(Position a, Position b) {
        List<Position> out = new ArrayList<>();
        int ax = a.getX(), ay = a.getY();
        int bx = b.getX(), by = b.getY();

        boolean aligned =
                (ax == bx) || (ay == by) || (Math.abs(bx - ax) == Math.abs(by - ay));
        if (!aligned) return out;

        int dx = Integer.compare(bx, ax);
        int dy = Integer.compare(by, ay);

        int x = ax + dx, y = ay + dy;
        while (x != bx || y != by) {
            out.add(new Position(x, y));
            x += dx; y += dy;
        }
        return out;
    }

    // Liefert Korridor zwischen eigener Figur und Angreifer (exkl. start, inkl. Angreifer), sonst leer.
    private List<Position> getKingConnectionPostion(Position startPos) {
        ChessPiece startPiece = board.getPiece(startPos.getY(), startPos.getX());
        if (startPiece == null) return List.of();

        Position attackerPos = checkStateAktiveTeam(startPos, startPiece);
        if (attackerPos == null) return List.of();

        List<Position> corridor = squaresBetweenExclusive(startPos, attackerPos);
        corridor.add(attackerPos);
        return corridor;
    }

    // Aktualisiert Rochaderechte nach Bewegung/Schlag eines Königs/Turms (inkl. geschlagenem Gegner-Turm).
    private void updateCastlingRightsOnMove(Position start, Position end, ChessPiece moving, CapturedInfo captured) {
        if (moving.getColour() == TeamColor.WHITE) {
            if (moving.getType() == PieceType.KING) {
                castlingRights.setWhiteKingSide(false);
                castlingRights.setWhiteQueenSide(false);
            } else if (moving.getType() == PieceType.ROOK) {
                if (start.getY() == 0 && start.getX() == 0) castlingRights.setWhiteQueenSide(false); // A1
                if (start.getY() == 0 && start.getX() == 7) castlingRights.setWhiteKingSide(false);  // H1
            }
        } else {
            if (moving.getType() == PieceType.KING) {
                castlingRights.setBlackKingSide(false);
                castlingRights.setBlackQueenSide(false);
            } else if (moving.getType() == PieceType.ROOK) {
                if (start.getY() == 7 && start.getX() == 0) castlingRights.setBlackQueenSide(false); // A8
                if (start.getY() == 7 && start.getX() == 7) castlingRights.setBlackKingSide(false);  // H8
            }
        }

        if (captured != null && captured.getType() == PieceType.ROOK) {
            TeamColor victim = captured.getColour();
            Position vpos = captured.getPosition();
            if (victim == TeamColor.WHITE) {
                if (vpos.getY() == 0 && vpos.getX() == 0) castlingRights.setWhiteQueenSide(false); // A1
                if (vpos.getY() == 0 && vpos.getX() == 7) castlingRights.setWhiteKingSide(false);  // H1
            } else {
                if (vpos.getY() == 7 && vpos.getX() == 0) castlingRights.setBlackQueenSide(false); // A8
                if (vpos.getY() == 7 && vpos.getX() == 7) castlingRights.setBlackKingSide(false);  // H8
            }
        }
    }
}
