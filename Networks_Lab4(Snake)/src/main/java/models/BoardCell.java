package models;

public class BoardCell {
    private int x;
    private int y;
    private BoardCellType boardCellType;

    public BoardCell(int x, int y, BoardCellType boardCellType){
        this.x = x;
        this.y = y;
        this.boardCellType = boardCellType;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public BoardCellType getBoardCellType() {
        return boardCellType;
    }

    public void setBoardCellType(BoardCellType boardCellType){
        this.boardCellType = boardCellType;
    }
}
