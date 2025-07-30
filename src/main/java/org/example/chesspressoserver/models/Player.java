package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player {
    public String playerId;
    public String name;

    public Player(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }

}
