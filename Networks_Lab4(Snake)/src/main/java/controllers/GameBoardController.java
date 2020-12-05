package controllers;

import gui.GameBoardView;
import models.GameBoardModel;
import models.GameProcess;
import models.ServerGameProcess;
import net.ClientGameProcess;
import net.MulticastSender;
import net.UnicastReceiver;
import net.UnicastSender;
import observers.Observer;
import protocols.SnakeProto.*;

import javax.swing.*;
import java.net.InetSocketAddress;

public class GameBoardController implements Observer {
    private GameBoardView gameBoardView;
    private GameBoardModel gameBoardModel;
    private StatusController statusController;
    private GameProcess gameProcess;
    private MulticastSender multicastSender;
    private UnicastSender unicastSender;
    private UnicastReceiver unicastReceiver;

    public GameBoardController(GameBoardView gameBoardView, GameBoardModel gameBoardModel, StatusController statusController,
                               MulticastSender multicastSender, UnicastSender unicastSender, UnicastReceiver unicastReceiver){
        this.gameBoardView = gameBoardView;
        this.gameBoardModel = gameBoardModel;
        this.statusController = statusController;
        this.multicastSender = multicastSender;
        this.unicastSender = unicastSender;
        this.unicastReceiver = unicastReceiver;
    }

    public JPanel getGameBoardPanel(){
        return gameBoardView.getGameBoardPanel();
    }

    public void createServerGame(GameConfig gameConfig){
        gameProcess = new ServerGameProcess(gameBoardModel, gameBoardView, multicastSender, gameConfig, unicastSender, unicastReceiver);
        gameProcess.createGame("Name");
        gameProcess.start();
        unicastReceiver.setGameProcess(gameProcess);
    }

    public void createClientGame(GameConfig gameConfig, InetSocketAddress master){
        gameProcess = new ClientGameProcess(gameBoardModel, gameBoardView, unicastSender, unicastReceiver, gameConfig, master);
        gameProcess.createGame("Name");
        gameProcess.start();
        unicastReceiver.setGameProcess(gameProcess);
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    @Override
    public void update(Object arg) {
        switch ((String) arg) {
            case "PressedW" -> {
                gameProcess.turnUp();
            }
            case "PressedA" -> {
                gameProcess.turnLeft();
            }
            case "PressedS" -> {
                gameProcess.turnDown();
            }
            case "PressedD" -> {
                gameProcess.turnRight();
            }
        }
    }
}
