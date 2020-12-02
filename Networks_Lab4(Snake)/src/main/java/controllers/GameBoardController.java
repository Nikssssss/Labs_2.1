package controllers;

import gui.GameBoardView;
import models.GameBoardModel;
import models.GameProcess;
import models.ServerGameProcess;
import observers.Observer;
import protocols.SnakeProto.*;

import javax.swing.*;

public class GameBoardController implements Observer {
    private GameBoardView gameBoardView;
    private GameBoardModel gameBoardModel;
    private StatusController statusController;
    private GameProcess gameProcess;

    public GameBoardController(GameBoardView gameBoardView, GameBoardModel gameBoardModel, StatusController statusController){
        this.gameBoardView = gameBoardView;
        this.gameBoardModel = gameBoardModel;
        this.statusController = statusController;
    }

    public JPanel getGameBoardPanel(){
        return gameBoardView.getGameBoardPanel();
    }

    public void createGame(){
        gameProcess = new ServerGameProcess(gameBoardModel, gameBoardView, 1000);
        gameProcess.createGame();
        gameProcess.start();
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
