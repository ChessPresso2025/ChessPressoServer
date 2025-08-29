package org.example.chesspressoserver.gamelogic;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.gamelogic.modles.CastlingRights;
import org.example.chesspressoserver.models.gamemodels.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameControllerTest {

    private GameController controller;
    private Board boardMock;

    @BeforeEach
    void setUp() {
        boardMock = mock(Board.class);
        controller = new GameController();
        controller.setBoard(boardMock);
        controller.setAktiveTeam(TeamColor.WHITE);
        controller.setCastlingRights(new CastlingRights());
    }

    @Test
    void testGetMovesForRequestReturnsEmptyIfNoPiece() {
        Position pos = new Position(0, 0);
        when(boardMock.getPiece(pos.getY(), pos.getX())).thenReturn(null);

        List<Position> moves = controller.getMovesForRequest(pos);
        assertTrue(moves.isEmpty());
    }

    @Test
    void testGetMovesForRequestReturnsEmptyIfWrongTeam() {
        Position pos = new Position(0, 0);
        ChessPiece piece = new ChessPiece(PieceType.PAWN, TeamColor.BLACK);
        when(boardMock.getPiece(pos.getY(), pos.getX())).thenReturn(piece);

        List<Position> moves = controller.getMovesForRequest(pos);
        assertTrue(moves.isEmpty());
    }

    @Test
    void testApplyMoveThrowsIfNoPiece() {
        Position start = new Position(0, 0);
        Position end = new Position(0, 1);
        when(boardMock.getPiece(start.getY(), start.getX())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> controller.applyMove(start, end, null));
    }

    @Test
    void testApplyMoveThrowsIfWrongTeam() {
        Position start = new Position(0, 0);
        Position end = new Position(0, 1);
        ChessPiece piece = new ChessPiece(PieceType.PAWN, TeamColor.BLACK);
        when(boardMock.getPiece(start.getY(), start.getX())).thenReturn(piece);

        assertThrows(IllegalStateException.class, () -> controller.applyMove(start, end, null));
    }

    @Test
    void testApplyMoveNormalMove() {
        Position start = new Position(1, 1);
        Position end = new Position(1, 2);
        ChessPiece piece = new ChessPiece(PieceType.PAWN, TeamColor.WHITE);
        when(boardMock.getPiece(start.getY(), start.getX())).thenReturn(piece);
        when(boardMock.getPiece(end.getY(), end.getX())).thenReturn(null);

        Move move = controller.applyMove(start, end, null);

        assertEquals(start, move.getStart());
        assertEquals(end, move.getEnd());
        assertEquals(PieceType.PAWN, move.getPiece());
        verify(boardMock).removePiece(start.getY(), start.getX());
        verify(boardMock).setPiece(end.getY(), end.getX(), piece);
    }

    @Test
    void testApplyMoveEnPassant() {
        // Setup: Weißer Bauer auf e5, schwarzer Bauer zieht von d7 nach d5 (Doppelzug)
        Position whitePawnPos = new Position(4, 4); // e5
        Position blackPawnStart = new Position(3, 6); // d7
        Position blackPawnEnd = new Position(3, 4); // d5

        ChessPiece whitePawn = new ChessPiece(PieceType.PAWN, TeamColor.WHITE);
        ChessPiece blackPawn = new ChessPiece(PieceType.PAWN, TeamColor.BLACK);

        when(boardMock.getPiece(4, 4)).thenReturn(whitePawn); // e5
        when(boardMock.getPiece(4, 3)).thenReturn(blackPawn); // d5
        when(boardMock.getPiece(4, 4)).thenReturn(whitePawn); // e5

        // Simuliere letzten Zug: Schwarz hat Doppelzug gemacht
        Move lastMove = new Move(blackPawnStart, blackPawnEnd, PieceType.PAWN);
        controller.setLastMove(lastMove);

        // En Passant: Weißer Bauer schlägt auf d6
        Position epTarget = new Position(3, 5); // d6
        when(boardMock.getPiece(5, 3)).thenReturn(null); // d6 leer

        Move move = controller.applyMove(whitePawnPos, epTarget, null);

        assertEquals(SpezialMove.EnPassnt, move.getSpezialMove());
        assertNotNull(move.getCaptured());
        assertEquals(PieceType.PAWN, move.getCaptured().getType());
        verify(boardMock).removePiece(4, 4); // Weißer Bauer zieht
        verify(boardMock).removePiece(4, 3); // Schwarzer Bauer wird entfernt (En Passant)
        verify(boardMock).setPiece(5, 3, whitePawn); // Weißer Bauer landet auf d6
    }

    @Test
    void testApplyMoveShortCastling() {
        // Setup: Weißer König auf e1, weißer Turm auf h1
        Position kingStart = new Position(4, 0); // e1
        Position kingEnd = new Position(6, 0);   // g1 (kurze Rochade)
        ChessPiece king = new ChessPiece(PieceType.KING, TeamColor.WHITE);
        ChessPiece rook = new ChessPiece(PieceType.ROOK, TeamColor.WHITE);

        when(boardMock.getPiece(0, 4)).thenReturn(king); // e1
        when(boardMock.getPiece(0, 7)).thenReturn(rook); // h1
        when(boardMock.checkEmpty(0, 5)).thenReturn(true); // f1 leer
        when(boardMock.checkEmpty(0, 6)).thenReturn(true); // g1 leer
        when(boardMock.checkEmpty(0, 4)).thenReturn(true); // e1 leer nach Zug

        controller.getCastlingRights().setWhiteKingSide(true);

        Move move = controller.applyMove(kingStart, kingEnd, null);

        assertEquals(SpezialMove.Castling, move.getSpezialMove());
        verify(boardMock).removePiece(0, 4); // König zieht
        verify(boardMock).removePiece(0, 7); // Turm zieht
        verify(boardMock).setPiece(0, 6, king); // König auf g1
        verify(boardMock).setPiece(0, 5, rook); // Turm auf f1
        assertFalse(controller.getCastlingRights().isWhiteKingSide());
        assertFalse(controller.getCastlingRights().isWhiteQueenSide());
    }

    @Test
    void testApplyMoveLongCastling() {
        // Setup: Weißer König auf e1, weißer Turm auf a1
        Position kingStart = new Position(4, 0); // e1
        Position kingEnd = new Position(2, 0);   // c1 (lange Rochade)
        ChessPiece king = new ChessPiece(PieceType.KING, TeamColor.WHITE);
        ChessPiece rook = new ChessPiece(PieceType.ROOK, TeamColor.WHITE);

        when(boardMock.getPiece(0, 4)).thenReturn(king); // e1
        when(boardMock.getPiece(0, 0)).thenReturn(rook); // a1
        when(boardMock.checkEmpty(0, 1)).thenReturn(true); // b1 leer
        when(boardMock.checkEmpty(0, 2)).thenReturn(true); // c1 leer
        when(boardMock.checkEmpty(0, 3)).thenReturn(true); // d1 leer
        when(boardMock.checkEmpty(0, 4)).thenReturn(true); // e1 leer nach Zug

        controller.getCastlingRights().setWhiteQueenSide(true);

        Move move = controller.applyMove(kingStart, kingEnd, null);

        assertEquals(SpezialMove.Castling, move.getSpezialMove());
        verify(boardMock).removePiece(0, 4); // König zieht
        verify(boardMock).removePiece(0, 0); // Turm zieht
        verify(boardMock).setPiece(0, 2, king); // König auf c1
        verify(boardMock).setPiece(0, 3, rook); // Turm auf d1
        assertFalse(controller.getCastlingRights().isWhiteKingSide());
        assertFalse(controller.getCastlingRights().isWhiteQueenSide());
    }

    @Test
    void testInitialKnightMoves() {
        // Test für das weiße Pferd auf b1
        Position whiteKnightPosition = new Position(1, 0); // b1 Position
        when(boardMock.getPiece(whiteKnightPosition.getY(), whiteKnightPosition.getX()))
                .thenReturn(new ChessPiece(PieceType.KNIGHT, TeamColor.WHITE));

        List<Position> whiteMoves = controller.getMovesForRequest(whiteKnightPosition);

        // Ein Pferd kann von b1 aus nach a3 und c3 ziehen
        assertTrue(whiteMoves.contains(new Position(0, 2))); // a3
        assertTrue(whiteMoves.contains(new Position(2, 2))); // c3
        assertEquals(2, whiteMoves.size()); // Nur diese zwei Züge sind möglich

        // Test für das weiße Pferd auf g1
        Position whiteKnightPosition2 = new Position(6, 0); // g1 Position
        when(boardMock.getPiece(whiteKnightPosition2.getY(), whiteKnightPosition2.getX()))
                .thenReturn(new ChessPiece(PieceType.KNIGHT, TeamColor.WHITE));

        List<Position> whiteMoves2 = controller.getMovesForRequest(whiteKnightPosition2);

        // Ein Pferd kann von g1 aus nach f3 und h3 ziehen
        assertTrue(whiteMoves2.contains(new Position(5, 2))); // f3
        assertTrue(whiteMoves2.contains(new Position(7, 2))); // h3
        assertEquals(2, whiteMoves2.size()); // Nur diese zwei Züge sind möglich
    }
}
