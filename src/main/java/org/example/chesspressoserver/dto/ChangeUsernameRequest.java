package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangeUsernameRequest {
    @NotBlank
    @Size(min = 3, max = 32)
    private String newUsername;

}
