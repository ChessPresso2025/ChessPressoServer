package org.example.chesspressoserver.gamelogic;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.gamelogic.modles.CastlingRights;
import org.example.chesspressoserver.models.gamemodels.*;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Getter
public class GameController {
    private UUID gameId;

    @Setter
    private Board board;

    @Setter
    private TeamColor aktiveTeam;

    @Setter
    private CastlingRights castlingRights = new CastlingRights();

    @Setter
    private Move lastMove;

    @Setter
    private int movesSincePawnMove = 0;  // Zähler für Züge ohne Bauernzug

    @Setter
    private int movesSinceCapture = 0;   // Zähler für Züge ohne Schlagen

    private List<Position> currentAttackers = new ArrayList<>(); // Liste der aktuellen Angreifer

    // Konstruktor
    public GameController() {
        this.board = new Board();
        this.aktiveTeam = TeamColor.WHITE;
        this.castlingRights = new CastlingRights();
        this.lastMove = null;
        board.start();
        this.movesSincePawnMove = 0;
        this.movesSinceCapture = 0;
    }

    public GameController(UUID gameId) {
        this();
        this.gameId = gameId;
    }

    // =====================================================================
    // 1) REQUEST: alle LEGALEN Züge für die Figur an startPos
    // =====================================================================

    // Methode für die Requesteingabe, gibt alle möglichen Moves an den Client
    public List<Position> getMovesForRequest(final Position startPos) {
        // Hole die Schachfigur von der angegebenen Startposition
        ChessPiece piece = board.getPiece(startPos.getX(), startPos.getY());
        // Bestimme die Teamfarben: eigenes Team und gegnerisches Team
        TeamColor me = piece.getColour();
        // Hole alle möglichen Züge der Figur (unabhängig von Schachregeln)
        List<Position> moves = new ArrayList<>(piece.getMove().getPossibleMoves(startPos, board));
        //Wenn der König gefragt wird, wird sofort die Liste zurück gesendet
        if(piece.getType() == PieceType.KING){
            return getFilteredKingMoves(startPos, moves);
        }
        if(piece.getType() == PieceType.PAWN){
            moves = getFilteredPawnMoves(startPos, moves);
        }


        // Filtere die Züge basierend auf Schachregeln
        // 1. Prüfe ob der König im Doppelschach steht
        if(currentAttackers.size() > 1){
                return new ArrayList<>();
        }
        // 2. Prüfe ob der König im einfachen Schach steht
        else if(currentAttackers.size() == 1){
            Position attacker = currentAttackers.get(0);
                List<Position> kingAttackerLine = hasClearLineList(attacker, board.getKingPosition(me));
                List<Position> legalMoves = new ArrayList<>();
                for (Position pos : moves) {
                    if (kingAttackerLine.contains(pos)) {
                        legalMoves.add(pos);
                    }
                }
                return legalMoves;
        }
        // 3. Kein Schach
        else {
            List<Position> attackers =  isPieceBetweenKingAndAttacker(startPos, board.getKingPosition(me));
            if(!attackers.isEmpty()){
                Position attacker = attackers.get(0);
                List<Position> kingAttackerLine = hasClearLineList(attacker, board.getKingPosition(me));
                List<Position> legalMoves = new ArrayList<>();
                for (Position pos : moves) {
                    if (kingAttackerLine.contains(pos)) {
                        legalMoves.add(pos);
                    }
                }
                return legalMoves;
            }
            return moves;
        }
    }

    public List<String> getMovesForRequestAsString(final Position position) {
        List<Position> positions = getMovesForRequest(position);
        List<String> moves = new ArrayList<>();
        for(Position p : positions) {
            moves.add(p.toString());
        }
        return moves;
    }

    // =====================================================================
    // 2) ZUG AUSFÜHREN: wendet einen legalen Zug an und gibt Info zurück
    // =====================================================================

    // Methode setzt den Move, welcher vom Client kommt um und sendet die Info an den Client zurück
    public Move applyMove(final Position start, final Position end, PieceType promotionChoice) {
        // 1) Grundvalidierung und bewegte Figur holen
        ChessPiece moving = board.getPiece(start.getX(), start.getY());
        if (moving == null || moving.getColour() != aktiveTeam) {
            throw new IllegalStateException("No active-team piece at start.");
        }
        TeamColor me = moving.getColour();
        TeamColor enemy = (me == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        Move result = new Move(start, end, moving.getType());

        // 2) Capture am Ziel (normal)
        ChessPiece targetAtEnd = board.getPiece(end.getX(), end.getY());
        if (targetAtEnd != null && targetAtEnd.getColour() != moving.getColour()) {
            result.setCaptured(new CapturedInfo(targetAtEnd.getType(), targetAtEnd.getColour(), end));
        }

        boolean isPawn   = (moving.getType() == PieceType.PAWN);
        boolean isCastle = (moving.getType() == PieceType.KING) && (Math.abs(end.getX() - start.getX()) == 2);
        boolean isEP     = false;

        // 3) Sonderzüge erkennen: En Passant
        Position epVictimPos = null;
        if (isPawn && lastMove != null && lastMove.getPiece() == PieceType.PAWN && targetAtEnd == null) {
            TeamColor enemyColor = enemy;
            Position from = lastMove.getStart();
            Position to   = lastMove.getEnd();
            int enemyTwoFromY = (enemyColor == TeamColor.WHITE) ? 1 : 6;
            int enemyTwoToY   = (enemyColor == TeamColor.WHITE) ? 3 : 4;

            boolean wasDoublePush = (from.getY() == enemyTwoFromY && to.getY() == enemyTwoToY && from.getX() == to.getX());
            boolean sideBySide    = (start.getY() == to.getY() && Math.abs(start.getX() - to.getX()) == 1);
            int expectedEPy       = (me == TeamColor.WHITE) ? (to.getY() + 1) : (to.getY() - 1);

            if (wasDoublePush && sideBySide && end.getX() == to.getX() && end.getY() == expectedEPy) {
                isEP = true;
                epVictimPos = to;
                ChessPiece epVictim = board.getPiece(epVictimPos.getX(), epVictimPos.getY());
                if (epVictim != null) {
                    result.setCaptured(new CapturedInfo(epVictim.getType(), epVictim.getColour(), epVictimPos));
                }
                result.setSpezialMove(SpezialMove.EN_PASSANT);
            }
        }

        // 4) Sonderzüge erkennen: Promotion
        boolean isPromotion = false;
        if (isPawn) {
            if ((moving.getColour() == TeamColor.WHITE && end.getY() == 7) ||
                (moving.getColour() == TeamColor.BLACK && end.getY() == 0)) {
                isPromotion = true;
                result.setSpezialMove(SpezialMove.PAWN_PROMOTION);
            }
        }

        // 5) 50-Züge-Regel Zähler
        if (isPawn) {
            movesSincePawnMove = 0;
        } else {
            movesSincePawnMove++;
        }
        if (targetAtEnd != null || isEP) {
            movesSinceCapture = 0;
        } else {
            movesSinceCapture++;
        }

        // 6) Rochaderechte vor dem tatsächlichen Setzen prüfen/aktualisieren
        updateCastlingRightsForMovedPiece(moving, start);
        if (targetAtEnd != null) {
            // Falls ein gegnerischer Turm auf seinem Startfeld geschlagen wurde, Rechte anpassen
            updateCastlingRightsForCapturedPiece(targetAtEnd, end);
        }

        // 7) Brett aktualisieren (Reihenfolge beachten)
        // En Passant: geschlagener Bauer wird vom Nachbarfeld entfernt
        if (isEP && epVictimPos != null) {
            board.removePiece(epVictimPos.getX(), epVictimPos.getY());
        }

        // Normales Schlagen am Endfeld (falls vorhanden)
        if (targetAtEnd != null && targetAtEnd.getColour() != moving.getColour()) {
            board.removePiece(end.getX(), end.getY());
        }

        // Bewege die Figur
        board.removePiece(start.getX(), start.getY());
        ChessPiece pieceToPlace = moving;
        if (isPromotion) {
            // Falls keine Wahl übergeben wurde, standardmäßig Dame
            PieceType promoteTo = (promotionChoice != null) ? promotionChoice : PieceType.QUEEN;
            pieceToPlace = new ChessPiece(promoteTo, moving.getColour());
        }
        board.setPiece(end.getX(), end.getY(), pieceToPlace);

        // Rochade: Turm umsetzen
        if (isCastle) {
            performCastling(start, end);
            result.setSpezialMove(SpezialMove.CASTLING);
        }

        // 8) En-Passant-Verfügbarkeit für den Gegner setzen (nach unserem Zug)
        setEnPassantIfDoublePush(moving, start, end);

        // 9) Letzten Zug speichern
        lastMove = result;

        // 10) Zugrecht wechseln
        aktiveTeam = enemy;

        // 11) currentAttackers für den neuen Spielerzug berechnen
        isInCheck(aktiveTeam); // füllt currentAttackers bei Bedarf

        return result;
    }

    // =====================================================================
    // Königsmovement
    // =====================================================================

    // Hauptmethode für Königsbewegungen: Filtert die Königszüge und fügt Rochade-Möglichkeiten hinzu.
    public List<Position> getFilteredKingMoves(Position kingPos, List<Position> possibleMoves) {
        List<Position> legalMoves = new ArrayList<>();

        // Hole Liste aller vom Gegner angegriffenen Felder
        List<Position> enemyAttacks = giveEnemyAttackList();

        // Filtere alle Züge, die in angegriffene Felder führen würden
        for (Position move : possibleMoves) {
            // Prüfe ob das Zielfeld vom Gegner angegriffen wird
            if (!enemyAttacks.contains(move)) {
                // Zusätzliche Prüfung: Wenn König eine gegnerische Figur schlagen würde,
                // darf diese nicht von einer anderen gegnerischen Figur beschützt werden
                if (isKingMoveValid(move)) {
                    legalMoves.add(move);
                }
            }
        }

        // Prüfe und füge Rochade-Möglichkeiten hinzu
        List<Position> castlingMoves = getAvailableCastlingMoves(kingPos);
        legalMoves.addAll(castlingMoves);

        return legalMoves;
    }

    // Prüft ob der König auf ein bestimmtes Feld ziehen kann.
    private boolean isKingMoveValid(Position targetPos) {
        ChessPiece targetPiece = board.getPiece(targetPos);

        // Wenn das Feld leer ist, kann der König dorthin ziehen
        if (targetPiece == null) {
            return true;
        }

        // Wenn eine eigene Figur dort steht, kann der König nicht dorthin
        if (targetPiece.getColour() == aktiveTeam) {
            return false;
        }

        // Wenn eine gegnerische Figur dort steht, prüfe ob sie von anderen Figuren beschützt wird
        if (targetPiece.getColour() != aktiveTeam) {
            return !isPositionProtectedByEnemy(targetPos);
        }

        return true;
    }

    // Prüft ob eine Position von einer gegnerischen Figur beschützt wird.
    private boolean isPositionProtectedByEnemy(Position pos) {
        TeamColor enemy = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // Durchsuche alle 64 Felder nach gegnerischen Figuren
        for(int y = 0; y < 8; y++) {
            for(int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);

                // Wenn eine gegnerische Figur gefunden wurde
                if(piece != null && piece.getColour() == enemy) {
                    Position piecePos = new Position(x, y);

                    // Hole alle möglichen Züge/Angriffe dieser gegnerischen Figur
                    List<Position> moves = piece.getMove().getPossibleMoves(piecePos, board);

                    // Wenn die Figur die geprüfte Position angreifen kann
                    if(moves != null && moves.contains(pos)) {
                        return true; // Position ist von dieser gegnerischen Figur beschützt
                    }
                }
            }
        }

        return false; // Position ist von keiner gegnerischen Figur beschützt
    }

    // Prüft alle Rochade-Möglichkeiten und gibt die verfügbaren Königszielfelder zurück.
    private List<Position> getAvailableCastlingMoves(Position kingPos) {
        List<Position> castlingMoves = new ArrayList<>();

        // Rochade ist nur möglich wenn König nicht im Schach steht
        if (!currentAttackers.isEmpty()) {
            return castlingMoves;
        }

        // Hole alle vom Gegner angegriffenen Felder für Rochade-Prüfung
        List<Position> enemyAttacks = giveEnemyAttackList();

        // Prüfe kurze Rochade (Königsseite: O-O)
        if (canPerformKingSideCastling(kingPos, enemyAttacks)) {
            castlingMoves.add(new Position(6, kingPos.getY())); // König landet auf G1/G8
        }

        // Prüfe lange Rochade (Damenseite: O-O-O)
        if (canPerformQueenSideCastling(kingPos, enemyAttacks)) {
            castlingMoves.add(new Position(2, kingPos.getY())); // König landet auf C1/C8
        }

        return castlingMoves;
    }

    // Prüft ob kurze Rochade (Königsseite) durchgeführt werden kann
    private boolean canPerformKingSideCastling(Position kingPos, List<Position> enemyAttacks) {
        // Prüfe ob Rochade-Rechte vorhanden sind
        boolean hasRights = (aktiveTeam == TeamColor.WHITE) ?
            castlingRights.isWhiteKingSide() : castlingRights.isBlackKingSide();

        if (!hasRights) {
            return false;
        }

        int y = kingPos.getY();

        // Prüfe ob die Felder zwischen König und Turm frei sind
        if (!board.checkEmpty(5, y) || !board.checkEmpty(6, y)) {
            return false;
        }

        // Prüfe ob der Turm vorhanden und unversehrt ist
        ChessPiece rook = board.getPiece(7, y);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColour() != aktiveTeam) {
            return false;
        }

        // Prüfe ob König-Startfeld, Durchgangsfeld und Zielfeld nicht angegriffen sind
        Position kingStart = new Position(4, y);
        Position kingPassage = new Position(5, y);
        Position kingTarget = new Position(6, y);

        return !enemyAttacks.contains(kingStart) &&
               !enemyAttacks.contains(kingPassage) &&
               !enemyAttacks.contains(kingTarget);
    }

    // Prüft ob lange Rochade (Damenseite) durchgeführt werden kann.
    private boolean canPerformQueenSideCastling(Position kingPos, List<Position> enemyAttacks) {
        // Prüfe ob Rochade-Rechte vorhanden sind
        boolean hasRights = (aktiveTeam == TeamColor.WHITE) ?
            castlingRights.isWhiteQueenSide() : castlingRights.isBlackQueenSide();

        if (!hasRights) {
            return false;
        }

        int y = kingPos.getY();

        // Prüfe ob die Felder zwischen König und Turm frei sind
        if (!board.checkEmpty(1, y) || !board.checkEmpty(2, y) || !board.checkEmpty(3, y)) {
            return false;
        }

        // Prüfe ob der Turm vorhanden und unversehrt ist
        ChessPiece rook = board.getPiece(0, y);
        if (rook == null || rook.getType() != PieceType.ROOK || rook.getColour() != aktiveTeam) {
            return false;
        }

        // Prüfe ob König-Startfeld, Durchgangsfeld und Zielfeld nicht angegriffen sind
        Position kingStart = new Position(4, y);
        Position kingPassage = new Position(3, y);
        Position kingTarget = new Position(2, y);

        return !enemyAttacks.contains(kingStart) &&
               !enemyAttacks.contains(kingPassage) &&
               !enemyAttacks.contains(kingTarget);
    }

    // Aktualisiert die Rochaderechte nach einem Königszug (beide Rechte der Farbe entfallen)
    private void adjustCastlingRightsAfterKingMove(TeamColor color) {
        if (color == TeamColor.WHITE) {
            castlingRights.setWhiteKingSide(false);
            castlingRights.setWhiteQueenSide(false);
        } else {
            castlingRights.setBlackKingSide(false);
            castlingRights.setBlackQueenSide(false);
        }
    }

    // Führt die Rochade (Turmbewegung) aus, falls kingStart→kingEnd ein Rochadezug ist
    // Rückgabe: true, wenn Turm erfolgreich umgesetzt wurde; sonst false
    private boolean performCastling(Position kingStart, Position kingEnd) {
        if (kingStart == null || kingEnd == null) return false;
        if (kingStart.getY() != kingEnd.getY()) return false;
        if (Math.abs(kingEnd.getX() - kingStart.getX()) != 2) return false; // kein Rochadezug

        int y = kingStart.getY();

        // Ermittele Königsfarbe (nach dem Move könnte der König bereits auf kingEnd stehen)
        ChessPiece king = board.getPiece(kingEnd);
        if (king == null || king.getType() != PieceType.KING) {
            king = board.getPiece(kingStart);
        }
        TeamColor color = (king != null) ? king.getColour() : aktiveTeam;

        if (kingEnd.getX() == 6) { // kurze Rochade: Turm von (7,y) nach (5,y)
            ChessPiece rook = board.getPiece(7, y);
            if (rook == null || rook.getType() != PieceType.ROOK || rook.getColour() != color) return false;
            board.removePiece(7, y);
            board.setPiece(5, y, rook);
            adjustCastlingRightsAfterKingMove(color);
            return true;
        } else if (kingEnd.getX() == 2) { // lange Rochade: Turm von (0,y) nach (3,y)
            ChessPiece rook = board.getPiece(0, y);
            if (rook == null || rook.getType() != PieceType.ROOK || rook.getColour() != color) return false;
            board.removePiece(0, y);
            board.setPiece(3, y, rook);
            adjustCastlingRightsAfterKingMove(color);
            return true;
        }
        return false;
    }

    // Aktualisiert Rochaderechte abhängig von der bewegten Figur (König/Turm)
    // Diese Methode sollte VOR dem eigentlichen Board-Move (Startfeld wird geleert) aufgerufen werden,
    // damit die Figur noch am Startfeld abgelesen werden kann – oder die Figur wird als Parameter übergeben.
    private void updateCastlingRightsForMovedPiece(ChessPiece movedPiece, Position startPos) {
        if (movedPiece == null || startPos == null) return;
        PieceType type = movedPiece.getType();
        TeamColor color = movedPiece.getColour();

        if (type == PieceType.KING) {
            // König bewegt: beide Rochaderechte der Farbe verlieren
            adjustCastlingRightsAfterKingMove(color);
            return;
        }

        if (type == PieceType.ROOK) {
            int sx = startPos.getX();
            int sy = startPos.getY();
            if (color == TeamColor.WHITE) {
                // Weißer Turm auf Startfeldern: a1=(0,0) → lange; h1=(7,0) → kurze
                if (sx == 0 && sy == 0) {
                    castlingRights.setWhiteQueenSide(false);
                } else if (sx == 7 && sy == 0) {
                    castlingRights.setWhiteKingSide(false);
                }
            } else {
                // Schwarzer Turm auf Startfeldern: a8=(0,7) → lange; h8=(7,7) → kurze
                if (sx == 0 && sy == 7) {
                    castlingRights.setBlackQueenSide(false);
                } else if (sx == 7 && sy == 7) {
                    castlingRights.setBlackKingSide(false);
                }
            }
        }
    }

    // Optional: Rechte anpassen, falls ein Turm auf seinem Startfeld geschlagen wurde
    private void updateCastlingRightsForCapturedPiece(ChessPiece capturedPiece, Position capturedPos) {
        if (capturedPiece == null || capturedPos == null) return;
        if (capturedPiece.getType() != PieceType.ROOK) return;

        int cx = capturedPos.getX();
        int cy = capturedPos.getY();
        TeamColor color = capturedPiece.getColour();

        if (color == TeamColor.WHITE) {
            if (cx == 0 && cy == 0) {
                castlingRights.setWhiteQueenSide(false);
            } else if (cx == 7 && cy == 0) {
                castlingRights.setWhiteKingSide(false);
            }
        } else {
            if (cx == 0 && cy == 7) {
                castlingRights.setBlackQueenSide(false);
            } else if (cx == 7 && cy == 7) {
                castlingRights.setBlackKingSide(false);
            }
        }
    }

    // =====================================================================
    // Bauernmovement
    // =====================================================================

    // Hauptmethode für Bauernbewegungen: Filtert die Bauernzüge und fügt En-Passant-Möglichkeiten hinzu.
    public List<Position> getFilteredPawnMoves(Position pawnPos, List<Position> possibleMoves) {
        List<Position> legalMoves = new ArrayList<>(possibleMoves);

        // Prüfe ob En-Passant möglich ist und füge es hinzu
        addEnPassantToFilteredMoves(pawnPos, legalMoves);

        return legalMoves;
    }

    // Überprüft ob En-Passant möglich ist und fügt den entsprechenden Zug hinzu
    private void addEnPassantToFilteredMoves(Position pawnPos, List<Position> moves) {
        // En-Passant ist nur möglich wenn ein letzter Zug existiert
        if (lastMove == null) {
            return;
        }

        ChessPiece ownPawn = board.getPiece(pawnPos.getX(), pawnPos.getY());
        if (ownPawn == null || ownPawn.getType() != PieceType.PAWN) {
            return;
        }

        TeamColor ownColor = ownPawn.getColour();
        TeamColor enemyColor = (ownColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // Der letzte Zug muss ein Bauernzug gewesen sein
        if (lastMove.getPiece() != PieceType.PAWN) {
            return;
        }

        Position enemyStart = lastMove.getStart();
        Position enemyEnd = lastMove.getEnd();

        // Prüfe ob der gegnerische Bauer einen Doppelzug gemacht hat
        if (!wasEnemyPawnDoubleMove(enemyStart, enemyEnd, enemyColor)) {
            return;
        }

        // Prüfe ob unser Bauer neben dem gegnerischen Bauern steht
        if (!isOwnPawnAdjacentToEnemyPawn(pawnPos, enemyEnd)) {
            return;
        }

        // Berechne die En-Passant-Zielposition
        Position enPassantTarget = calculateEnPassantTarget(enemyEnd, ownColor);

        // Prüfe ob das En-Passant-Zielfeld frei ist
        if (enPassantTarget != null && board.checkEmpty(enPassantTarget.getX(), enPassantTarget.getY())) {
            moves.add(enPassantTarget);
        }
    }

    // Prüft ob der gegnerische Bauer einen Doppelzug gemacht hat
    private boolean wasEnemyPawnDoubleMove(Position start, Position end, TeamColor enemyColor) {
        // Für weißen Bauern: von Reihe 1 nach Reihe 3 (y: 1 -> 3)
        // Für schwarzen Bauern: von Reihe 6 nach Reihe 4 (y: 6 -> 4)
        int expectedStartY = (enemyColor == TeamColor.WHITE) ? 1 : 6;
        int expectedEndY = (enemyColor == TeamColor.WHITE) ? 3 : 4;

        return start.getY() == expectedStartY &&
               end.getY() == expectedEndY &&
               start.getX() == end.getX() &&
               Math.abs(end.getY() - start.getY()) == 2;
    }

    // Prüft ob unser Bauer neben dem gegnerischen Bauern steht
    private boolean isOwnPawnAdjacentToEnemyPawn(Position ownPawnPos, Position enemyPawnPos) {
        // Bauern müssen in derselben Reihe stehen
        if (ownPawnPos.getY() != enemyPawnPos.getY()) {
            return false;
        }

        // Bauern müssen direkt nebeneinander stehen (Abstand 1 in x-Richtung)
        return Math.abs(ownPawnPos.getX() - enemyPawnPos.getX()) == 1;
    }

    // Berechnet die En-Passant-Zielposition
    private Position calculateEnPassantTarget(Position enemyPawnPos, TeamColor ownColor) {
        // En-Passant-Zielfeld ist das übersprungene Feld des gegnerischen Bauern
        int targetX = enemyPawnPos.getX();
        int targetY;

        if (ownColor == TeamColor.WHITE) {
            // Weißer Bauer schlägt nach vorne (y + 1)
            targetY = enemyPawnPos.getY() + 1;
        } else {
            // Schwarzer Bauer schlägt nach vorne (y - 1)
            targetY = enemyPawnPos.getY() - 1;
        }

        // Prüfe ob die Zielposition auf dem Brett liegt
        if (targetX >= 0 && targetX < 8 && targetY >= 0 && targetY < 8) {
            return new Position(targetX, targetY);
        }

        return null;
    }

    // Setzt den En-Passant-Zustand nach einem Zug, indem lastMove aktualisiert wird.
    // Gibt true zurück, wenn der Zug ein Bauern-Doppelzug war (En-Passant für den Gegner möglich), sonst false.
    public boolean setEnPassantIfDoublePush(ChessPiece movedPiece, Position start, Position end) {
        if (movedPiece == null || start == null || end == null) return false;

        // lastMove MUSS immer auf den letzten Zug gesetzt werden, damit EP nicht aus einem älteren Zug bleibt
        lastMove = new Move(start, end, movedPiece.getType());

        if (movedPiece.getType() != PieceType.PAWN) return false;
        TeamColor color = movedPiece.getColour();

        // Doppelzug-Bedingungen
        if (color == TeamColor.WHITE) {
            return start.getY() == 1 && end.getY() == 3 && start.getX() == end.getX();
        } else {
            return start.getY() == 6 && end.getY() == 4 && start.getX() == end.getX();
        }
    }

    // =====================================================================
    // Hilfsmethoden
    // =====================================================================

    // Erstellt eine Liste zwischen den Feldern bei einer direkten Verbindung
    public List<Position> hasClearLineList(Position pos1, Position pos2) {
        List<Position> linePositions = new ArrayList<>();

        // Prüfe ob Positionen identisch sind
        if (pos1.equals(pos2)) {
            return linePositions; // Leere Liste zurückgeben
        }

        int x1 = pos1.getX(), y1 = pos1.getY();
        int x2 = pos2.getX(), y2 = pos2.getY();

        // Prüfe ob die Positionen auf einer geraden Linie liegen (horizontal, vertikal oder diagonal)
        boolean isHorizontal = (y1 == y2);
        boolean isVertical = (x1 == x2);
        boolean isDiagonal = (Math.abs(x2 - x1) == Math.abs(y2 - y1));

        if (!isHorizontal && !isVertical && !isDiagonal) {
            return linePositions; // Keine direkte Verbindung
        }

        // Berechne Richtungsvektoren
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);

        // Hole Figuren an den beiden Positionen
        ChessPiece piece1 = board.getPiece(pos1);
        ChessPiece piece2 = board.getPiece(pos2);

        // Prüfe ob pos1 hinzugefügt werden soll:
        // - Liste darf nicht null sein (ist automatisch erfüllt da wir eine neue Liste erstellen)
        // - Beide Positionen müssen Figuren haben
        // - Die Figuren müssen unterschiedliche Farben haben
        boolean shouldIncludePos1 = (piece1 != null && piece2 != null &&
                                    piece1.getColour() != piece2.getColour());

        if (shouldIncludePos1) {
            linePositions.add(pos1);
        }

        // Durchlaufe alle Positionen zwischen pos1 und pos2 (exklusive)
        int currentX = x1 + dx;
        int currentY = y1 + dy;

        while (currentX != x2 || currentY != y2) {
            // Prüfe ob Position noch auf dem Brett ist
            if (currentX < 0 || currentX > 7 || currentY < 0 || currentY > 7) {
                break;
            }

            Position currentPos = new Position(currentX, currentY);
            linePositions.add(currentPos);

            currentX += dx;
            currentY += dy;
        }

        return linePositions;
    }

    // Methode zum Überprüfen ob eine Direkte Verbindung zwischen zwei Positionen besteht
    private boolean hasClearLineBoolean(Position pos1, Position pos2) {
        if (pos1.equals(pos2)) return false;
        int x1 = pos1.getX(), y1 = pos1.getY();
        int x2 = pos2.getX(), y2 = pos2.getY();
        boolean aligned = (x1 == x2) || (y1 == y2) || (Math.abs(x2 - x1) == Math.abs(y2 - y1));
        if (!aligned) return false;
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);
        int x = x1 + dx, y = y1 + dy;
        while (x != x2 || y != y2) {
            if (!board.checkEmpty(x, y)) return false;
            x += dx; y += dy;
        }
        return true;
    }

    // Liste aller angegriffenen Felder des gegnerischen Teams
    public List<Position> giveEnemyAttackList() {
        List<Position> attackedSquares = new ArrayList<>();
        TeamColor enemy = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece != null && piece.getColour() == enemy) {
                    Position piecePos = new Position(x, y);
                    List<Position> moves = piece.getMove().getPossibleMoves(piecePos, board);
                    if (moves != null) {
                        for (Position attackedPos : moves) {
                            if (!attackedSquares.contains(attackedPos)) {
                                attackedSquares.add(attackedPos);
                            }
                        }
                    }
                }
            }
        }
        return attackedSquares;
    }

    //Methode zum Überprüfen ob ein Figur zwischen König und Angreifer steht (Pinning)
    private List<Position> isPieceBetweenKingAndAttacker(Position piecePos, Position kingPos){
        List<Position> attackersInLine = new ArrayList<>();
        if(!hasClearLineBoolean(piecePos, kingPos)){
            return attackersInLine; // Leere Liste wenn keine direkte Linie
        }
        ChessPiece piece = board.getPiece(piecePos.getX(), piecePos.getY());
        if(piece == null) {
            return attackersInLine;
        }
        // Simuliere das Entfernen der Figur
        board.removePiece(piecePos.getX(), piecePos.getY());
        TeamColor enemy = (piece.getColour() == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        int dx = Integer.compare(piecePos.getX() - kingPos.getX(), 0);
        int dy = Integer.compare(piecePos.getY() - kingPos.getY(), 0);
        int x = piecePos.getX() + dx;
        int y = piecePos.getY() + dy;
        while (x >= 0 && x < 8 && y >= 0 && y < 8) {
            ChessPiece potentialAttacker = board.getPiece(x, y);
            if (potentialAttacker != null) {
                if (potentialAttacker.getColour() == enemy) {
                    if (canPieceAttackInDirection(potentialAttacker, dx, dy)) {
                        attackersInLine.add(new Position(x, y));
                        break; // Nur einen Angreifer in dieser Richtung möglich
                    }
                }
                break; // Erste Figur stoppt die Suche in diese Richtung
            }
            x += dx; y += dy;
        }
        // Figur wieder einsetzen
        board.setPiece(piecePos.getX(), piecePos.getY(), piece);
        return attackersInLine;
    }

    // Hilfsmethode: Prüft ob eine Figur in eine bestimmte Richtung angreifen kann
    private boolean canPieceAttackInDirection(ChessPiece piece, int dx, int dy) {
        PieceType type = piece.getType();
        switch (type) {
            case QUEEN:
                return true; // alle Richtungen
            case ROOK:
                return (dx == 0 || dy == 0);
            case BISHOP:
                return (Math.abs(dx) == Math.abs(dy) && dx != 0 && dy != 0);
            default:
                return false;
        }
    }


    //prüft auf Schach und füllt currentAttackers
    public boolean isInCheck(TeamColor teamColor) {
        Position kingPos = board.getKingPosition(teamColor);
        if (kingPos == null) return false;
        TeamColor enemy = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        currentAttackers.clear();
        boolean inCheck = false;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece != null && piece.getColour() == enemy) {
                    Position pos = new Position(x, y);
                    List<Position> moves = piece.getMove().getPossibleMoves(pos, board);
                    if (moves != null && moves.contains(kingPos)) {
                        if (!currentAttackers.contains(pos)) currentAttackers.add(pos);
                        inCheck = true;
                    }
                }
            }
        }
        return inCheck;
    }

    // Gibt eine Liste aller Angreifer auf den König zurück
    public List<Position> getAttackersOnKing(TeamColor teamColor) {
        Position kingPos = board.getKingPosition(teamColor);
        List<Position> attackers = new ArrayList<>();
        if (kingPos == null) return attackers;
        TeamColor enemy = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece != null && piece.getColour() == enemy) {
                    Position pos = new Position(x, y);
                    List<Position> moves = piece.getMove().getPossibleMoves(pos, board);
                    if (moves != null && moves.contains(kingPos)) {
                        attackers.add(pos);
                    }
                }
            }
        }
        return attackers;
    }


    //prüft auf ein Schachmatt
    public boolean isCheckmate(TeamColor teamColor) {
        Position kingPos = board.getKingPosition(teamColor);
        if (kingPos == null) return false;
        List<Position> kingMoves = getFilteredKingMoves(kingPos, board.getPiece(kingPos).getMove().getPossibleMoves(kingPos, board));
        // 1. Doppelschach: Nur König kann sich retten
        if (currentAttackers.size() > 1) {
            return kingMoves.isEmpty();
        }
        // 2. Einfaches Schach: König, Schlagen oder Blocken
        if (currentAttackers.size() == 1) {
            Position attackerPos = currentAttackers.get(0);
            // a) Kann König sich retten?
            if (!kingMoves.isEmpty()) return false;
            // b) Kann Angreifer geschlagen werden oder blockiert werden?
            List<Position> blockSquares = hasClearLineList(attackerPos, kingPos);
            blockSquares.add(attackerPos); // Angreifer kann auch direkt geschlagen werden
            // Prüfe alle eigenen Figuren außer König
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    ChessPiece piece = board.getPiece(x, y);
                    if (piece != null && piece.getColour() == teamColor && piece.getType() != PieceType.KING) {
                        Position piecePos = new Position(x, y);
                        List<Position> moves = piece.getMove().getPossibleMoves(piecePos, board);
                        for (Position move : moves) {
                            if (blockSquares.contains(move)) {
                                // Simuliere Zug und prüfe, ob neues Schach entsteht
                                ChessPiece temp = board.getPiece(move.getX(), move.getY());
                                board.removePiece(piecePos.getX(), piecePos.getY());
                                board.setPiece(move.getX(), move.getY(), piece);
                                boolean stillInCheck;
                                if(getAttackersOnKing(teamColor).isEmpty()){;
                                    stillInCheck = false;
                                } else {
                                    stillInCheck = true;
                                }
                                // Rückgängig machen
                                board.setPiece(piecePos.getX(), piecePos.getY(), piece);
                                board.setPiece(move.getX(), move.getY(), temp);
                                if (!stillInCheck) return false;
                            }
                        }
                    }
                }
            }
            // Kein Zug möglich: Schachmatt
            return true;
        }
        // 3. Kein Schach
        return false;
    }


    //Gibt zurück, ob eine Patt-Situation vorliegt
    public boolean isStalemate(TeamColor team) {
        // Ein Patt liegt vor, wenn der König nicht im Schach steht und das aktive Team keine legalen Züge mehr hat
        Position kingPos = board.getKingPosition(team);
        if (kingPos == null) return false;

        // Prüfe ob der König im Schach steht
        if (!getAttackersOnKing(team).isEmpty()) {
            return false; // König steht im Schach, also kein Patt
        }

        if (noMovesLeft(team)) {
            return true; // Kein legaler Zug mehr, also Patt
        }
        if (isFiftyMoveRule()){
            return true; // 50-Züge-Regel greift, also Patt
        }
        if (noCheckPossible()){
            return true; // Kein Schachmatt mehr möglich, also Patt
        }
        return false; // Sonst kein Patt
    }


    // Prüft, ob das aktive Team keine legalen Züge mehr hat (Patt-Situation)
    public boolean noMovesLeft(TeamColor team) {
        for(int x = 0; x < 8; x++) {
            for(int y = 0; y < 8; y++) {
                Position pos = new Position(x, y);
                ChessPiece piece = board.getPiece(x, y);
                if(piece != null && piece.getColour() == team) {
                    List<String> possibleMoves = getMovesForRequestAsString(pos);
                    if(!possibleMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Prüft ob die 50-Züge-Regel greift (50 Züge ohne Bauernzug und ohne Schlagen)
    public boolean isFiftyMoveRule() {
        return movesSincePawnMove >= 50 && movesSinceCapture >= 50;
    }

    // Prüft, ob Schach noch möglich ist
    public boolean noCheckPossible() {
        // Zähle die Figuren für jedes Team
        int[] whitePieces = countPieces(TeamColor.WHITE);
        int[] blackPieces = countPieces(TeamColor.BLACK);

        // Prüfe verschiedene "nicht-schachmatt-mögliche" Konstellationen:

        // 1. Nur Könige
        if (isOnlyKing(whitePieces) && isOnlyKing(blackPieces)) {
            return true;
        }

        // 2. König + einzelner Springer vs. König
        if ((isOnlyKingAndOneKnight(whitePieces) && isOnlyKing(blackPieces)) ||
                (isOnlyKing(whitePieces) && isOnlyKingAndOneKnight(blackPieces))) {
            return true;
        }

        // 3. König + zwei Springer vs. König
        if ((isOnlyKingAndTwoKnights(whitePieces) && isOnlyKing(blackPieces)) ||
                (isOnlyKing(whitePieces) && isOnlyKingAndTwoKnights(blackPieces))) {
            return true;
        }

        // 4. König + einzelner Läufer vs. König
        if ((isOnlyKingAndOneBishop(whitePieces) && isOnlyKing(blackPieces)) ||
                (isOnlyKing(whitePieces) && isOnlyKingAndOneBishop(blackPieces))) {
            return true;
        }

        // 5. König + Läufer vs. König + Läufer (gleiche Farbe)
        if (isOnlyKingAndOneBishop(whitePieces) && isOnlyKingAndOneBishop(blackPieces)) {
            // Prüfe ob die Läufer auf gleicher Feldfarbe sind
            Position whiteBishop = findBishop(TeamColor.WHITE);
            Position blackBishop = findBishop(TeamColor.BLACK);

            if (whiteBishop != null && blackBishop != null) {
                // Läufer auf gleicher Feldfarbe wenn Summe der Koordinaten gleiche Parität hat
                boolean whiteSquareSum = (whiteBishop.getX() + whiteBishop.getY()) % 2 == 0;
                boolean blackSquareSum = (blackBishop.getX() + blackBishop.getY()) % 2 == 0;
                if (whiteSquareSum == blackSquareSum) {
                    return true;
                }
            }
        }

        return false;
    }

    // Hilfsmethoden für noCheckPossible:

    // Zählt die Anzahl jeder Figurenart für ein Team
    private int[] countPieces(TeamColor color) {
        // Index entspricht PieceType.ordinal(): PAWN,KNIGHT,BISHOP,ROOK,QUEEN,KING
        int[] pieces = new int[6];

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece != null && piece.getColour() == color) {
                    pieces[piece.getType().ordinal()]++;
                }
            }
        }
        return pieces;
    }

    // Findet die Position des Läufers einer Farbe
    private Position findBishop(TeamColor color) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                ChessPiece piece = board.getPiece(x, y);
                if (piece != null &&
                        piece.getColour() == color &&
                        piece.getType() == PieceType.BISHOP) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    // Prüft, ob nur ein König vorhanden ist
    private boolean isOnlyKing(int[] pieces) {
        return pieces[PieceType.KING.ordinal()] == 1 &&
               pieces[PieceType.PAWN.ordinal()] == 0 &&
               pieces[PieceType.KNIGHT.ordinal()] == 0 &&
               pieces[PieceType.BISHOP.ordinal()] == 0 &&
               pieces[PieceType.ROOK.ordinal()] == 0 &&
               pieces[PieceType.QUEEN.ordinal()] == 0;
    }

    // Prüft, ob nur ein König und ein Springer vorhanden sind
    private boolean isOnlyKingAndOneKnight(int[] pieces) {
        return pieces[PieceType.KING.ordinal()] == 1 &&
               pieces[PieceType.KNIGHT.ordinal()] == 1 &&
               pieces[PieceType.PAWN.ordinal()] == 0 &&
               pieces[PieceType.BISHOP.ordinal()] == 0 &&
               pieces[PieceType.ROOK.ordinal()] == 0 &&
               pieces[PieceType.QUEEN.ordinal()] == 0;
    }

    // Prüft, ob nur ein König und zwei Springer vorhanden sind
    private boolean isOnlyKingAndTwoKnights(int[] pieces) {
        return pieces[PieceType.KING.ordinal()] == 1 &&
               pieces[PieceType.KNIGHT.ordinal()] == 2 &&
               pieces[PieceType.PAWN.ordinal()] == 0 &&
               pieces[PieceType.BISHOP.ordinal()] == 0 &&
               pieces[PieceType.ROOK.ordinal()] == 0 &&
               pieces[PieceType.QUEEN.ordinal()] == 0;
    }

    // Prüft, ob nur ein König und ein Läufer vorhanden sind
    private boolean isOnlyKingAndOneBishop(int[] pieces) {
        return pieces[PieceType.KING.ordinal()] == 1 &&
               pieces[PieceType.BISHOP.ordinal()] == 1 &&
               pieces[PieceType.PAWN.ordinal()] == 0 &&
               pieces[PieceType.KNIGHT.ordinal()] == 0 &&
               pieces[PieceType.ROOK.ordinal()] == 0 &&
               pieces[PieceType.QUEEN.ordinal()] == 0;
    }
}
