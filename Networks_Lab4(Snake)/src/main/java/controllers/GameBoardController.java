package controllers;

import exceptions.NoEmptyCellException;
import gui.GameBoardView;
import models.GameBoardModel;
import models.GameProcess;
import observers.Observer;
import protocols.SnakeProto.*;

import javax.swing.*;
import java.util.Objects;
import java.util.Random;

public class GameBoardController implements Observer {
    private GameBoardView gameBoardView;
    private GameBoardModel gameBoardModel;
    private StatusController statusController;
    private Random random = new Random();

    public GameBoardController(GameBoardView gameBoardView, GameBoardModel gameBoardModel, StatusController statusController){
        this.gameBoardView = gameBoardView;
        this.gameBoardModel = gameBoardModel;
        this.statusController = statusController;
    }

    public JPanel getGameBoardPanel(){
        return gameBoardView.getGameBoardPanel();
    }

    public void createGame(){
        gameBoardModel.createBoard(50, 50);
        gameBoardView.setGameBoardSize(50, 50);
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
        GameProcess gameProcess = new GameProcess(gameBoardModel, gameBoardView, 1000);
        Thread gameProcessThread = new Thread(gameProcess);
        gameProcessThread.start();
    }

    public GameState.Snake createSnake() throws NoEmptyCellException {
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

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    @Override
    public void update(Object arg) {
        if (gameBoardModel.getMasterAddress() == null) {
            switch ((String) arg) {
                case "PressedW" -> {
                    gameBoardModel.changeSnakeDirection(0, Direction.UP);
                }
                case "PressedA" -> {
                    gameBoardModel.changeSnakeDirection(0, Direction.LEFT);
                }
                case "PressedS" -> {
                    gameBoardModel.changeSnakeDirection(0, Direction.DOWN);
                }
                case "PressedD" -> {
                    gameBoardModel.changeSnakeDirection(0, Direction.RIGHT);
                }
            }
        }
    }
}
