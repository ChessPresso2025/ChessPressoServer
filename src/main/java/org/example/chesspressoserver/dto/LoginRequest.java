package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    // Getter und Setter
    @NotBlank(message = "Login (Username oder Email) ist erforderlich")
    private String login;  // kann username oder email sein

    @NotBlank(message = "Passwort ist erforderlich")
    private String password;

}
