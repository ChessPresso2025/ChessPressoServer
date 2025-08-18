package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

public class AuthResponse {
    @Getter
    @Setter
    private String playerId;

    @Getter
    @Setter
    private String name;

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

    @Getter
    @Setter
    private String email;

    public AuthResponse() {}

    public AuthResponse(String playerId, String name, int playedGames, int win, int draw, int lose, String email) {
        this.playerId = playerId;
        this.name = name;
        this.playedGames = playedGames;
        this.win = win;
        this.draw = draw;
        this.lose = lose;
        this.email = email;
    }
}
