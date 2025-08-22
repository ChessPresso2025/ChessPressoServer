package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StatsReportRequest {

    @NotBlank(message = "Result ist erforderlich")
    @Pattern(regexp = "WIN|LOSS|DRAW", message = "Result muss WIN, LOSS oder DRAW sein")
    private String result;
}
