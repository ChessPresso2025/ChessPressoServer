package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

@Setter @Getter
public class PrivateLobby extends Lobby {
    private TeamColor player1Colour;
    private TeamColor player2Colour;

    //Constructor
    public PrivateLobby(String lobbyID, String player1ID) {
        super(lobbyID, player1ID);
        setPlayer1Colour(TeamColor.WHITE);
    }
}
