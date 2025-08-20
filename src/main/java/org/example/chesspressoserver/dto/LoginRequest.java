package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Login (Username oder Email) ist erforderlich")
    private String login;  // kann username oder email sein

    @NotBlank(message = "Passwort ist erforderlich")
    private String password;

    // Getter und Setter
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
