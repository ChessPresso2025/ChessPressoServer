package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

@Getter
public class Position {

    private final String pos; // e.g. "A1"
    private final int x;      // col 0..7  (A..H)
    private final int y;      // row 0..7  (1..8)

    // constructor from algebraic like "A1" or "h8"
    public Position(String pos) {
        if (pos == null || pos.length() != 2) {
            throw new IllegalArgumentException("pos must be like A1..H8");
        }
        this.pos = pos.toUpperCase();
        this.x = calculateX(this.pos);
        this.y = calculateY(this.pos);
        ensureOnBoard(this.x, this.y);
    }

    // constructor from coordinates (x=col, y=row)
    public Position(int x, int y) {
        ensureOnBoard(x, y);
        this.x = x;
        this.y = y;
        this.pos = calculatePos(x, y);
    }

    private int calculateX(String pos) {
        char file = pos.charAt(0);
        if (file < 'A' || file > 'H') {
            throw new IllegalArgumentException("file must be A..H");
        }
        return file - 'A';
    }

    private int calculateY(String pos) {
        char rank = pos.charAt(1);
        if (rank < '1' || rank > '8') {
            throw new IllegalArgumentException("rank must be 1..8");
        }
        return rank - '1';
    }

    private String calculatePos(int x, int y) {
        char file = (char) (x + 'A');
        char rank = (char) (y + '1');
        return "" + file + rank;
    }

    private void ensureOnBoard(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("coordinates out of board: x=" + x + ", y=" + y);
        }
    }

    // --- value equality ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position other = (Position) o;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public String toString() {
        return pos;
    }
}
