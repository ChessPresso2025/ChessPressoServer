package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Lobby {

    protected String lobbyID;
    @Setter
    protected String player1ID;
    @Setter
    protected String player2ID;

    //Constructor
    public Lobby(String lobbyID, String player1ID) {
        this.lobbyID = lobbyID;
        this.player1ID = player1ID;
    }
}
