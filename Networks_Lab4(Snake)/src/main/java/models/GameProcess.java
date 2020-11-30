package models;

import exceptions.IllegalDirectionException;
import gui.GameBoardView;
import protocols.SnakeProto;
import protocols.SnakeProto.*;

import java.util.LinkedList;
import java.util.List;

public class GameProcess implements Runnable{
    private GameBoardModel gameBoardModel;
    private GameBoardView gameBoardView;
    private final int stateDelay;

    public GameProcess(GameBoardModel gameBoardModel, GameBoardView gameBoardView, int stateDelay) {
        this.gameBoardModel = gameBoardModel;
        this.gameBoardView = gameBoardView;
        this.stateDelay = stateDelay;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            gameBoardModel.clearBoard();
            //TODO place food
            for (int i = 0; i < gameBoardModel.getSnakes().size(); i++){
                GameState.Snake prevStepSnake = gameBoardModel.getSnakes().get(i);
                GameState.Snake nextStepSnake = moveSnake(prevStepSnake, gameBoardModel.getSnakeDirection(prevStepSnake.getPlayerId()));
                gameBoardModel.getSnakes().set(i, nextStepSnake);
            }
            gameBoardView.setCells(gameBoardModel.getBoardCells());
            gameBoardView.repaint();
            try {
                Thread.sleep(stateDelay);
            } catch (InterruptedException e) {
                break;
            }
        }
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

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }
}
