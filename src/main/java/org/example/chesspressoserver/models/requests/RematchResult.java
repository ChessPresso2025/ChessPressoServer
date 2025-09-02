package org.example.chesspressoserver.models.requests;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RematchResult {
    @Getter
    private String type = "rematch-result";
    @Setter
    @Getter
    private String lobbyId;
    private final String newlobbyid;
    @Setter
    @Getter
    private String result; // "accepted" oder "declined"

    public RematchResult(String lobbyId, String result, String newlobbyid) {
        this.lobbyId = lobbyId;
        this.result = result;
        this.newlobbyid = newlobbyid;
    }

}

