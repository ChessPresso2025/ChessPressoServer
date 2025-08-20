package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username ist erforderlich")
    @Size(min = 3, max = 50, message = "Username muss zwischen 3 und 50 Zeichen lang sein")
    private String username;

    @NotBlank(message = "Email ist erforderlich")
    @Email(message = "Ungültige Email-Adresse")
    private String email;

    @NotBlank(message = "Passwort ist erforderlich")
    @Size(min = 6, message = "Passwort muss mindestens 6 Zeichen lang sein")
    private String password;

    // Getter und Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
