package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Player {
    private String playerId;
    @Setter
    private String name;
    @Setter
    private int playedGames;
    @Setter
    private int win;
    @Setter
    private int draw;
    @Setter
    private int lose;

    //Constructor
    public Player(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }

    public double winPercentage() {
        return win * 100.0 / playedGames;
    }

    public double drawPercentage() {
        return draw * 100.0 / playedGames;
    }

    public double losePercentage() {
        return lose * 100.0 / playedGames;
    }
}
