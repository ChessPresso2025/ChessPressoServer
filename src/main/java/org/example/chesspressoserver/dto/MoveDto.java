package org.example.chesspressoserver.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MoveDto {
    public UUID id;
    public int moveNumber;
    public String moveNotation;
    public OffsetDateTime createdAt;
}
