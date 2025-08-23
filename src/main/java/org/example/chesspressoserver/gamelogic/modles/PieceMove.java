package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public abstract class PieceMove {
    Board board;

    public abstract List<Position> getPossibleMoves(Position pos);

}
