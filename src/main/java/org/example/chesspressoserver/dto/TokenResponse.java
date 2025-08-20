package org.example.chesspressoserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String username;

    public TokenResponse(String accessToken, long expiresIn, String username) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.username = username;
    }
}
