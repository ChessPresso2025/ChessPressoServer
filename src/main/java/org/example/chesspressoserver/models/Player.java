package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

public class Player {
    @Getter
    private String playerId;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String username;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private String email;
    @Getter
    @Setter
    private int playedGames;
    @Getter
    @Setter
    private int win;
    @Getter
    @Setter
    private int draw;
    @Getter
    @Setter
    private int lose;

    //Constructor for new login system
    public Player(String playerId, String name, String username, String password, String email) {
        this.playerId = playerId;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    //Constructor for backward compatibility
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
