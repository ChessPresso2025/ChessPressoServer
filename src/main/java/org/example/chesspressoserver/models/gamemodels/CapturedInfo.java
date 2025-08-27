package org.example.chesspressoserver.models.gamemodels;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CapturedInfo {
    private final PieceType type;     // Typ der geschlagenen Figur
    private final TeamColor colour;   // Farbe der geschlagenen Figur
    private final Position position;  // Feld, auf dem sie entfernt wurde (bei EP = lastMove.end)
}
