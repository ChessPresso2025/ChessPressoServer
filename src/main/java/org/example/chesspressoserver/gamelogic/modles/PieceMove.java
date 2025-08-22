package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public abstract class PieceMove {
    Board board;

    public abstract List<Position> getPossibleMoves(Position pos);


    //Allgemeine Movements
    public List<Position> horizontalRight(Position pos){
        List<Position> moves = new ArrayList<Position>();
        int posY = pos.getY();
        for(int i = pos.getX(); i < 8; i++){

        }
        return List.of();
    }

    public List<Position> horizontalLeft(Position pos){
        List<Position> moves = new ArrayList<Position>();
        int posY = pos.getY();
        for(int i = pos.getX(); i >= 0; i--){

        }
        return List.of();
    }

    public List<Position> verticalUp(Position pos){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        for(int i = pos.getY(); i >= 0; i--){

        }
        return List.of();
    }

    public List<Position> verticalDown(Position pos){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        for(int i = pos.getY(); i < 8; i++){

        }
        return List.of();
    }

    public List<Position> diagonalUpRight(Position pos){
        List<Position> moves = new ArrayList<Position>();

        return List.of();
    }

    public List<Position> diagonalUpLeft(Position pos){
        List<Position> moves = new ArrayList<Position>();

        return List.of();
    }

    public List<Position> diagonalDownRight(Position pos){
        List<Position> moves = new ArrayList<Position>();

        return List.of();
    }

    public List<Position> diagonalDownLeft(Position pos){
        List<Position> moves = new ArrayList<Position>();

        return List.of();
    }
}
