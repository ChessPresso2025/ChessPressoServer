package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

public class RegisterRequest {
    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;

    @Getter
    @Setter
    private String email;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
