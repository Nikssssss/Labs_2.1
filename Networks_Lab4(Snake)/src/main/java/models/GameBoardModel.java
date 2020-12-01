package models;

import exceptions.NoEmptyCellException;
import protocols.SnakeProto.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GameBoardModel {
    private int rows;
    private int columns;
    private ArrayList<BoardCell> boardCells;
    private ArrayList<GameState.Snake> snakes;
    private ConcurrentHashMap<Integer, Direction> playerSnakeDirection;
    private HashMap<GamePlayer, Integer> playersScore;
    private InetSocketAddress masterAddress;

    public GameBoardModel(){
        boardCells = new ArrayList<>();
        snakes = new ArrayList<>();
        playersScore = new HashMap<>();
        playerSnakeDirection = new ConcurrentHashMap<>();
    }

    public void createBoard(int rows, int columns){
        this.rows = rows;
        this.columns = columns;
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                boardCells.add(new BoardCell(j, i, BoardCellType.EMPTY));
            }
        }
    }

    public void clearSnakeCells(){
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                BoardCell currentBoardCell = boardCells.get(i * rows + j);
                if (currentBoardCell.getBoardCellType() == BoardCellType.SNAKE_BODY || currentBoardCell.getBoardCellType() == BoardCellType.SNAKE_HEAD) {
                    currentBoardCell.setBoardCellType(BoardCellType.EMPTY);
                }
            }
        }
    }

    public GameState.Coord getFreeCell() throws NoEmptyCellException {
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                BoardCell currentCell = boardCells.get(i * rows + j);
                if (isEmptySurrounding(currentCell)){
                    return coord(currentCell.getX(), currentCell.getY());
                }
            }
        }
        throw new NoEmptyCellException();
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    private boolean isEmptySurrounding(BoardCell cell){
        for (int k = (cell.getY() - 2); k < (cell.getY() + 2); k++){
            for (int s = (cell.getX() - 2); s < (cell.getX() + 2); s++){
                if (s < 0 || k < 0){
                    if (s < 0) s = columns + s;
                    if (k < 0) k = rows + k;
                }
                if (s > columns || k > rows){
                    if (s > columns) s = s - columns;
                    if (k > rows) k = rows - k;
                }
                if (boardCells.get(k * rows + s).getBoardCellType() != BoardCellType.EMPTY){
                    return false;
                }
            }
        }
        return true;
    }

    public void addSnake(GameState.Snake snake){
        snakes.add(snake);
    }

    public void removeSnake(GameState.Snake snake){
        snakes.remove(snake);
    }

    public void addSnakeCoordinates(List<GameState.Coord> coordinates){
        BoardCell currentCell = boardCells.get(coordinates.get(0).getY() * rows + coordinates.get(0).getX());
        if (currentCell.getBoardCellType() == BoardCellType.SNAKE_BODY || currentCell.getBoardCellType() == BoardCellType.SNAKE_HEAD) {
            currentCell.addBoardCellType(BoardCellType.SNAKE_HEAD);
        }
        else {
            currentCell.setBoardCellType(BoardCellType.SNAKE_HEAD);
        }
        for (int i = 1; i < coordinates.size(); i++){
            currentCell = boardCells.get(coordinates.get(i).getY() * rows + coordinates.get(i).getX());
            if (currentCell.getBoardCellType() == BoardCellType.SNAKE_BODY || currentCell.getBoardCellType() == BoardCellType.SNAKE_HEAD) {
                currentCell.addBoardCellType(BoardCellType.SNAKE_BODY);
            }
            else{
                currentCell.setBoardCellType(BoardCellType.SNAKE_BODY);
            }
        }
    }

    public void addAllFoodCoordinates(List<GameState.Coord> coordinates){
        for (var coordinate : coordinates){
            boardCells.get(coordinate.getY() * rows + coordinate.getX()).setBoardCellType(BoardCellType.FOOD);
        }
    }

    public void addFoodCoordinates(GameState.Coord coordinates){
        boardCells.get(coordinates.getY() * rows + coordinates.getX()).setBoardCellType(BoardCellType.FOOD);
    }

    public void emptyCell(GameState.Coord coordinates){
        boardCells.get(coordinates.getY() * rows + coordinates.getX()).setBoardCellType(BoardCellType.EMPTY);
    }

    public int getFoodCount(){
        int count = 0;
        for (var cell : boardCells){
            if (cell.getBoardCellType() == BoardCellType.FOOD){
                count++;
            }
        }
        return count;
    }

    public ArrayList<BoardCell> getBoardCells(){
        return boardCells;
    }

    public ArrayList<GameState.Snake> getSnakes(){
        return snakes;
    }

    public int getRows(){
        return rows;
    }

    public int getColumns(){
        return columns;
    }

    public boolean isFoodCell(int x, int y){
        return boardCells.get(rows * y + x).getBoardCellType() == BoardCellType.FOOD;
    }

    public boolean isEmptyCell(int x, int y){
        return boardCells.get(rows * y + x).getBoardCellType() == BoardCellType.EMPTY;
    }

    public int getEmptyCount(){
        int count = 0;
        for (var cell : boardCells){
            if (cell.getBoardCellType() == BoardCellType.EMPTY){
                count++;
            }
        }
        return count;
    }

    public void changeSnakeDirection(Integer playerId, Direction headDirection){
        if (playerSnakeDirection.containsKey(playerId)){
            playerSnakeDirection.replace(playerId, headDirection);
        }
        else {
            playerSnakeDirection.put(playerId, headDirection);
        }
    }

    public Direction getSnakeDirection(Integer playerId){
        return playerSnakeDirection.get(playerId);
    }

    public InetSocketAddress getMasterAddress(){
        return masterAddress;
    }

    public int getNumberOfPlayers(){
        return playersScore.size();
    }

    public int getNumberOfSnakes(){
        return snakes.size();
    }
}
