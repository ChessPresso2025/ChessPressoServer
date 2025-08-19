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

    private int calculateX(String pos) {
        return pos.charAt(0) - 'A';
    }

    private int calculateY(String pos) {
        return pos.charAt(1) - '1';
    }
}
