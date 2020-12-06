package models;

import java.util.ArrayList;

public class BoardCell {
    private final int x;
    private final int y;
    private final ArrayList<BoardCellType> boardCellTypes;

    public BoardCell(int x, int y, BoardCellType boardCellType){
        this.x = x;
        this.y = y;
        boardCellTypes = new ArrayList<>();
        setBoardCellType(boardCellType);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public BoardCellType getBoardCellType() {
        return boardCellTypes.get(0);
    }

    public void setBoardCellType(BoardCellType boardCellType){
        boardCellTypes.clear();
        boardCellTypes.add(boardCellType);
    }

    public void addBoardCellType(BoardCellType boardCellType){
        boardCellTypes.add(boardCellType);
    }

    public int getSnakesOnCellCount(){
        return boardCellTypes.size();
    }

    public boolean hasOnlyHeads(){
        for (var type : boardCellTypes){
            if (!(type == BoardCellType.ENEMY_HEAD || type == BoardCellType.OWN_HEAD)){
                return false;
            }
        }
        return true;
    }
}
