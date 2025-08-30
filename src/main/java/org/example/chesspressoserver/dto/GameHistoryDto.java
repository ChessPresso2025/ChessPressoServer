package org.example.chesspressoserver.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class GameHistoryDto {
    public UUID id;
    public OffsetDateTime startedAt;
    public OffsetDateTime endedAt;
    public String result;
    public List<MoveDto> moves;
}

