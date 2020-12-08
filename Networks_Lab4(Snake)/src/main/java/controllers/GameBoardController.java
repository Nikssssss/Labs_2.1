package controllers;

import gui.GameBoardView;
import launcher.IGameLauncher;
import models.GameBoardModel;
import gameprocess.GameProcess;
import gameprocess.ServerGameProcess;
import gameprocess.ClientGameProcess;
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
    private IGameLauncher gameLauncher;
    private final String name = "Nikita";

    public GameBoardController(GameBoardView gameBoardView, GameBoardModel gameBoardModel, StatusController statusController,
                               MulticastSender multicastSender, UnicastSender unicastSender, UnicastReceiver unicastReceiver,
                               IGameLauncher gameLauncher){
        this.gameBoardView = gameBoardView;
        this.gameBoardModel = gameBoardModel;
        this.statusController = statusController;
        this.multicastSender = multicastSender;
        this.unicastSender = unicastSender;
        this.unicastReceiver = unicastReceiver;
        this.gameLauncher = gameLauncher;
    }

    public JPanel getGameBoardPanel(){
        return gameBoardView.getGameBoardPanel();
    }

    public void createServerGame(GameConfig gameConfig){
        statusController.setPlayersScore(gameBoardModel.getPlayersScore());
        statusController.setGameConfig(gameConfig);
        statusController.setHostName(name);
        gameProcess = new ServerGameProcess(gameBoardModel, gameBoardView, multicastSender, gameConfig,
                unicastSender, unicastReceiver, gameLauncher, statusController);
        unicastReceiver.setGameProcess(gameProcess);
        gameProcess.createGame(name);
        gameProcess.start();
    }

    public void transformClientToServer(GameConfig gameConfig){
        statusController.setGameConfig(gameConfig);
        statusController.setHostName(name);
        gameProcess = new ServerGameProcess(gameBoardModel, gameBoardView, multicastSender, gameConfig,
                unicastSender, unicastReceiver, gameLauncher, statusController);
        unicastReceiver.setGameProcess(gameProcess);
        gameProcess.start();
    }

    public void createClientGame(GameConfig gameConfig, InetSocketAddress master){
        statusController.setHostName(master.getHostString());
        statusController.setGameConfig(gameConfig);
        statusController.setPlayersScore(gameBoardModel.getPlayersScore());
        gameProcess = new ClientGameProcess(gameBoardModel, gameBoardView, unicastSender,
                unicastReceiver, gameConfig, master, gameLauncher, statusController);
        unicastReceiver.setGameProcess(gameProcess);
        gameProcess.createGame(name);
        gameProcess.start();
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
            case "ExitGame" -> {
                gameProcess.exit();
            }
        }
    }
}
