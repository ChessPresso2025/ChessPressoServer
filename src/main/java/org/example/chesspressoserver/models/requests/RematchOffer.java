package org.example.chesspressoserver.models.requests;

import lombok.Getter;
import lombok.Setter;

public class RematchOffer {
    @Getter
    private String type = "rematch-offer";
    @Setter
    @Getter
    private String lobbyId;
    @Setter
    private String fromPlayerId;
    @Setter
    @Getter
    private String toPlayerId;


    public RematchOffer(String lobbyId, String fromPlayerId, String toPlayerId) {
        this.lobbyId = lobbyId;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
    }

}
