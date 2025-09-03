package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class RegisterRequest {

    // Getter und Setter
    @NotBlank(message = "Username ist erforderlich")
    @Size(min = 3, max = 50, message = "Username muss zwischen 3 und 50 Zeichen lang sein")
    private String username;

    @NotBlank(message = "Email ist erforderlich")
    @Email(message = "Ungültige Email-Adresse")
    private String email;

    @NotBlank(message = "Passwort ist erforderlich")
    @Size(min = 3, message = "Passwort muss mindestens 3 Zeichen lang sein")
    private String password;

}
