package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;
import java.util.List;

public interface PieceMove {

List<Position> getPossibleMoves(Position start);

}
