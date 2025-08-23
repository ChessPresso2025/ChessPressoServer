package org.example.chesspressoserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsResponse {
    private Integer wins;
    private Integer losses;
    private Integer draws;

    public Integer getTotal() {
        return wins + losses + draws;
    }
}
