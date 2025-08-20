package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

@Getter
public class Position {

    private String pos;
    private int x;
    private int y;

    //constructor
    public Position(String pos) {
        this.pos = pos;
        this.x = calculateX(this.pos);
        this.y = calculateY(this.pos);
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
        this.pos = calculatePos(x, y);
    }

    private int calculateX(String pos) {
        return pos.charAt(0) - 'A';
    }

    private int calculateY(String pos) {
        return pos.charAt(1) - '1';
    }

    private String calculatePos(int x, int y) {
        char file = (char) (x + 'A');
        char rank = (char) (y + '1');
        return "" + file + rank;
    }
}
