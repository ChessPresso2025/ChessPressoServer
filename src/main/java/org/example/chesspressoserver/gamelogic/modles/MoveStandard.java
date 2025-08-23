package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveStandard{

    //Allgemeine Movements
    public List<Position> horizontalRight(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posY = pos.getY();
        for(int i = pos.getX()+1; i < 8; i++){
            if(board.checkEmpty(i, posY)){
                moves.add(new Position(i, posY));
            }else{
                ChessPiece startPiece = board.getPiece(pos.getX(), posY);
                ChessPiece piece = board.getPiece(i, posY);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(i, posY));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> horizontalLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posY = pos.getY();
        for(int i = pos.getX()-1; i >= 0; i--){
            if(board.checkEmpty(i, posY)){
                moves.add(new Position(i, posY));
            }else{
                ChessPiece startPiece = board.getPiece(pos.getX(), posY);
                ChessPiece piece = board.getPiece(i, posY);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(i, posY));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> verticalUp(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        for(int i = pos.getY()-1; i >= 0; i--){
            if(board.checkEmpty(posX, i)){
                moves.add(new Position(posX, i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, pos.getY());
                ChessPiece piece = board.getPiece(posX, i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX, i));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> verticalDown(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        for(int i = pos.getY()+1; i < 8; i++){
            if(board.checkEmpty(posX, i)){
                moves.add(new Position(posX, i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, pos.getY());
                ChessPiece piece = board.getPiece(posX, i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX, i));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalUpRight(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        int posY = pos.getY();
        for(int i = 1; posX + i < 8 || posY + i < 8 ; i++) {
            if (board.checkEmpty(posX + i, posY + i)) {
                moves.add(new Position(posX + i, posY + i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, posY);
                ChessPiece piece = board.getPiece(posX + i, posY + i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX + i,posY + i));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalUpLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        int posY = pos.getY();
        for(int i = 1; posX - i >= 0 || posY + i < 8 ; i++) {
            if (board.checkEmpty(posX - i, posY + i)) {
                moves.add(new Position(posX - i, posY + i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, posY);
                ChessPiece piece = board.getPiece(posX - i, posY + i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX - i,posY + i));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalDownRight(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        int posY = pos.getY();
        for(int i = 1; posX + i < 8 || posY - i >= 0 ; i++) {
            if (board.checkEmpty(posX + i, posY - i)) {
                moves.add(new Position(posX + i, posY - i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, posY);
                ChessPiece piece = board.getPiece(posX + i, posY - i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX + i,posY - i));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalDownLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<Position>();
        int posX = pos.getX();
        int posY = pos.getY();
        for(int i = 1; posX - i >= 0 || posY - i >= 0 ; i++) {
            if (board.checkEmpty(posX - i, posY - i)) {
                moves.add(new Position(posX - i, posY - i));
            }else{
                ChessPiece startPiece = board.getPiece(posX, posY);
                ChessPiece piece = board.getPiece(posX - i, posY - i);
                if(startPiece.getColour() != piece.getColour()){
                    moves.add(new Position(posX - i,posY - i));
                }
                break;
            }
        }
        return moves;
    }
}
