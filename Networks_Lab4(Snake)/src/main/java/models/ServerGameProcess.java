package models;

import exceptions.IllegalDirectionException;
import exceptions.NoEmptyCellException;
import gui.GameBoardView;
import protocols.SnakeProto;
import protocols.SnakeProto.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ServerGameProcess implements GameProcess{
    private Process process;
    private Thread processThread;
    private GameBoardModel gameBoardModel;
    private GameBoardView gameBoardView;
    private final int stateDelay;
    private Random random = new Random();
    private final int foodStatic = 1;
    private final int foodPerPlayer = 2;
    private final double deadFoodProbability = 0.3;

    private class Process implements Runnable{
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                gameBoardModel.clearSnakeCells();
                for (int i = 0; i < gameBoardModel.getSnakes().size(); i++){
                    GameState.Snake prevStepSnake = gameBoardModel.getSnakes().get(i);
                    GameState.Snake nextStepSnake = moveSnake(prevStepSnake, gameBoardModel.getSnakeDirection(prevStepSnake.getPlayerId()));
                    gameBoardModel.getSnakes().set(i, nextStepSnake);
                }
                checkCollisions();
                placeFood();
                gameBoardView.setCells(gameBoardModel.getBoardCells());
                gameBoardView.repaint();
                try {
                    Thread.sleep(stateDelay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public ServerGameProcess(GameBoardModel gameBoardModel, GameBoardView gameBoardView, int stateDelay) {
        process = new Process();
        this.gameBoardModel = gameBoardModel;
        this.gameBoardView = gameBoardView;
        this.stateDelay = stateDelay;
    }

    @Override
    public void start(){
        processThread = new Thread(process);
        processThread.start();
    }

    @Override
    public void stop(){
        processThread.interrupt();
    }

    @Override
    public void createGame(){
        gameBoardModel.createBoard(20, 20);
        gameBoardView.setGameBoardSize(20, 20);
        gameBoardView.setCells(gameBoardModel.getBoardCells());
        GameState.Snake snake = null;
//        try {
//            snake = createSnake();
//            gameBoardModel.addSnake(snake);
//            gameBoardModel.changeSnakeDirection(snake.getPlayerId(), snake.getHeadDirection());
//        } catch (NoEmptyCellException e) {
//            e.printStackTrace();
//        }
        snake = GameState.Snake.newBuilder()
                .setPlayerId(0)
                .setHeadDirection(Direction.UP)
                .setState(GameState.Snake.SnakeState.ALIVE)
                .addPoints(coord(5, 5))
                .addPoints(coord(3,0))
                .addPoints(coord(0, 3))
                .addPoints(coord(5, 0))
                .build();
        gameBoardModel.addSnake(snake);
        gameBoardModel.changeSnakeDirection(snake.getPlayerId(), snake.getHeadDirection());
        placeFood();
    }

    private GameState.Snake createSnake() throws NoEmptyCellException {
        GameState.Coord snakeHead = gameBoardModel.getFreeCell();
        Direction headDirection = Direction.forNumber(random.nextInt(4) + 1);
        GameState.Coord snakeBody = null;
        switch (headDirection){
            case UP -> snakeBody = coord(snakeHead.getX(), snakeHead.getY() + 1);
            case DOWN -> snakeBody = coord(snakeHead.getX(), snakeHead.getY() - 1);
            case LEFT -> snakeBody = coord(snakeHead.getX() + 1, snakeHead.getY());
            case RIGHT -> snakeBody = coord(snakeHead.getX() - 1, snakeHead.getY());
        }
        return GameState.Snake.newBuilder()
                .setPlayerId(0)
                .setHeadDirection(headDirection)
                .setState(GameState.Snake.SnakeState.ALIVE)
                .addPoints(snakeHead)
                .addPoints(snakeBody)
                .build();
    }

    private GameState.Snake moveSnake(GameState.Snake snake, Direction newHeadDirection){
        List<GameState.Coord> compressedOldCoordinates = snake.getPointsList();
        List<GameState.Coord> fullOldCoordinates = getFullCoordinatesFrom(compressedOldCoordinates);
        GameState.Coord oldHeadCoordinates = snake.getPoints(0);
        GameState.Coord newHeadCoordinates = null;
        try {
            newHeadCoordinates = getNewHeadCoordinates(oldHeadCoordinates, newHeadDirection, snake.getHeadDirection());
        } catch (IllegalDirectionException e) {
            try {
                newHeadCoordinates = getNewHeadCoordinates(oldHeadCoordinates, snake.getHeadDirection(), snake.getHeadDirection());
                newHeadDirection = snake.getHeadDirection();
            } catch (IllegalDirectionException ignored) {}
        }
        List<GameState.Coord> fullNewCoordinates = new LinkedList<>();
        if (!gameBoardModel.isFoodCell(newHeadCoordinates.getX(), newHeadCoordinates.getY())){
            fullOldCoordinates.remove(fullOldCoordinates.size() - 1);
        }
        fullNewCoordinates.add(newHeadCoordinates);
        fullNewCoordinates.addAll(fullOldCoordinates);
        gameBoardModel.addSnakeCoordinates(fullNewCoordinates);
        return GameState.Snake.newBuilder()
                .setPlayerId(snake.getPlayerId())
                .setHeadDirection(newHeadDirection)
                .setState(GameState.Snake.SnakeState.ALIVE)
                .addAllPoints(getCompressedCoordinatesFrom(fullNewCoordinates, newHeadDirection))
                .build();
    }

    private GameState.Coord getNewHeadCoordinates(GameState.Coord headCoordinates, Direction newHeadDirection, Direction prevHeadDirection) throws IllegalDirectionException {
        switch(newHeadDirection){
            case UP -> {
                if (prevHeadDirection != Direction.DOWN) {
                    int y = headCoordinates.getY() - 1;
                    if (y < 0) y = gameBoardModel.getRows() + y;
                    return coord(headCoordinates.getX(), y);
                }
            }
            case DOWN -> {
                if (prevHeadDirection != Direction.UP) {
                    int y = headCoordinates.getY() + 1;
                    if (y > gameBoardModel.getRows() - 1) y = y - gameBoardModel.getRows();
                    return coord(headCoordinates.getX(), y);
                }
            }
            case RIGHT -> {
                if (prevHeadDirection != Direction.LEFT) {
                    int x = headCoordinates.getX() + 1;
                    if (x > gameBoardModel.getColumns() - 1) x = x - gameBoardModel.getColumns();
                    return coord(x, headCoordinates.getY());
                }
            }
            case LEFT -> {
                if (prevHeadDirection != Direction.RIGHT) {
                    int x = headCoordinates.getX() - 1;
                    if (x < 0) x = gameBoardModel.getColumns() + x;
                    return coord(x, headCoordinates.getY());
                }
            }
        }
        throw new IllegalDirectionException();
    }

    private List<SnakeProto.GameState.Coord> getFullCoordinatesFrom(List<SnakeProto.GameState.Coord> compressedCoordinates){
        List<SnakeProto.GameState.Coord> fullCoordinates = new LinkedList<>();
        SnakeProto.GameState.Coord lastTurnCoordinates = compressedCoordinates.get(0);
        fullCoordinates.add(lastTurnCoordinates);
        for (int i = 1; i < compressedCoordinates.size(); i++){
            SnakeProto.GameState.Coord currentCoordinates = compressedCoordinates.get(i);
            if (currentCoordinates.getX() == 0){
                if (currentCoordinates.getY() < 0) {
                    for (int j = -1; j >= currentCoordinates.getY(); j--) {
                        int y = lastTurnCoordinates.getY() + j;
                        if (y < 0) y = gameBoardModel.getRows() + y;
                        fullCoordinates.add(coord(lastTurnCoordinates.getX(), y));
                    }
                }
                else {
                    for (int j = 1; j <= currentCoordinates.getY(); j++) {
                        fullCoordinates.add(coord(lastTurnCoordinates.getX(), (lastTurnCoordinates.getY() + j) % gameBoardModel.getRows()));
                    }
                }
            }
            else {
                if (currentCoordinates.getX() < 0) {
                    for (int j = -1; j >= currentCoordinates.getX(); j--) {
                        int x = lastTurnCoordinates.getX() + j;
                        if (x < 0) x = gameBoardModel.getColumns() + x;
                        fullCoordinates.add(coord(x, lastTurnCoordinates.getY()));
                    }
                }
                else {
                    for (int j = 1; j <= currentCoordinates.getX(); j++) {
                        fullCoordinates.add(coord((lastTurnCoordinates.getX() + j) % gameBoardModel.getColumns(), lastTurnCoordinates.getY()));
                    }
                }
            }
            lastTurnCoordinates = fullCoordinates.get(fullCoordinates.size() - 1);
        }
        return fullCoordinates;
    }

    private List<SnakeProto.GameState.Coord> getCompressedCoordinatesFrom(List<SnakeProto.GameState.Coord> fullCoordinates, Direction headDirection){
        List<SnakeProto.GameState.Coord> compressedCoordinates = new LinkedList<>();
        compressedCoordinates.add(fullCoordinates.get(0));
        SnakeProto.GameState.Coord lastTurnCoordinates = fullCoordinates.get(0);
        for (int i = 1; i < fullCoordinates.size(); i++){
            SnakeProto.GameState.Coord currentCoordinates = fullCoordinates.get(i);
            SnakeProto.GameState.Coord nextCoordinates;
            int nextCell;
            int offset;
            if (currentCoordinates.getX() == lastTurnCoordinates.getX()){
                nextCell = i + 1;
                nextCoordinates = currentCoordinates;
                offset = 1;
                while (nextCell < fullCoordinates.size() && fullCoordinates.get(nextCell).getX() == lastTurnCoordinates.getX()){
                    nextCoordinates = fullCoordinates.get(nextCell);
                    nextCell++;
                    offset++;
                }
                int y;
                if (lastTurnCoordinates.getY() == gameBoardModel.getRows() - 1 && currentCoordinates.getY() == 0){
                    y = offset;
                }
                else if (lastTurnCoordinates.getY() == 0 && currentCoordinates.getY() == gameBoardModel.getRows() - 1){
                    y = -offset;
                }
                else if (lastTurnCoordinates.getY() - currentCoordinates.getY() < 0){
                    y = offset;
                }
                else {
                    y = -offset;
                }
                compressedCoordinates.add(coord(0, y));
                i = nextCell - 1;
                lastTurnCoordinates = nextCoordinates;
            }
            else {
                nextCell = i + 1;
                nextCoordinates = currentCoordinates;
                offset = 1;
                while (nextCell < fullCoordinates.size() && fullCoordinates.get(nextCell).getY() == lastTurnCoordinates.getY()){
                    nextCoordinates = fullCoordinates.get(nextCell);
                    nextCell++;
                    offset++;
                }
                int x;
                if (lastTurnCoordinates.getX() == gameBoardModel.getColumns() - 1 && currentCoordinates.getX() == 0){
                    x = offset;
                }
                else if (lastTurnCoordinates.getX() == 0 && currentCoordinates.getX() == gameBoardModel.getColumns() - 1){
                    x = -offset;
                }
                else if (lastTurnCoordinates.getX() - currentCoordinates.getX() < 0){
                    x = offset;
                }
                else {
                    x = -offset;
                }
                compressedCoordinates.add(coord(x, 0));
                i = nextCell - 1;
                lastTurnCoordinates = nextCoordinates;
            }
        }
        return compressedCoordinates;
    }

    private void checkCollisions(){
        List<GameState.Coord> deadSnakesCoordinates = new LinkedList<>();
        List<GameState.Snake> deadSnakes = new LinkedList<>();
        for (var cell : gameBoardModel.getBoardCells()){
            if (cell.getSnakesOnCellCount() > 1){
                var snakesIterator = gameBoardModel.getSnakes().iterator();
                while (snakesIterator.hasNext()){
                    var snake = snakesIterator.next();
                    if (snake.getPoints(0).equals(coord(cell.getX(), cell.getY()))){
                        deadSnakesCoordinates.addAll(getFullCoordinatesFrom(snake.getPointsList()));
                        deadSnakes.add(snake);
                    }
                    else if (getFullCoordinatesFrom(snake.getPointsList()).contains(coord(cell.getX(), cell.getY()))){
                        //TODO add point to player
                        System.out.println(snake.getPlayerId() + "has earned 1 point");
                    }
                }
            }
        }
        for (var coordinate : deadSnakesCoordinates){
            if (random.nextDouble() <= deadFoodProbability){
                gameBoardModel.addFoodCoordinates(coordinate);
            }
            else {
                gameBoardModel.emptyCell(coordinate);
            }
        }
        for (var snake : deadSnakes){
            gameBoardModel.removeSnake(snake);
        }
    }

    private void placeFood(){
        if (gameBoardModel.getFoodCount() != (foodStatic + foodPerPlayer * gameBoardModel.getNumberOfPlayers())){
            int requiredFood = (foodStatic + foodPerPlayer * gameBoardModel.getNumberOfSnakes()) - gameBoardModel.getFoodCount();
            int emptyCells = gameBoardModel.getEmptyCount();
            if (emptyCells < requiredFood){
                requiredFood = emptyCells;
            }
            ArrayList<BoardCell> cells = gameBoardModel.getBoardCells();
            while (requiredFood > 0){
                BoardCell currentCell = cells.get(random.nextInt(cells.size()));
                if (currentCell.getBoardCellType() == BoardCellType.EMPTY){
                    gameBoardModel.addFoodCoordinates(coord(currentCell.getX(), currentCell.getY()));
                    requiredFood--;
                }
            }
        }
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }
}
