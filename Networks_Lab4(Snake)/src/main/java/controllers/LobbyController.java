package controllers;

import gui.LobbyView;
import launcher.IGameLauncher;
import models.GameInfo;
import models.LobbyModel;
import observers.Observer;

import javax.swing.*;

public class LobbyController implements Observer {
    private LobbyView lobbyView;
    private LobbyModel lobbyModel;
    private IGameLauncher gameLauncher;

    public LobbyController(LobbyView lobbyView, LobbyModel lobbyModel, IGameLauncher gameLauncher){
        this.lobbyView = lobbyView;
        this.lobbyModel = lobbyModel;
        this.gameLauncher = gameLauncher;
    }

    public JPanel getLobbyPanel() {
        return lobbyView.getLobbyPanel();
    }

    @Override
    public void update(Object arg) {
        if (arg instanceof GameInfo){
            if (((GameInfo) arg).isActual()){
                lobbyView.addAvailableGame((GameInfo) arg);
            }
            else{
                lobbyView.removeAvailableGame((GameInfo) arg);
            }
        }
        else if (arg.equals("CreateGame")){
            gameLauncher.createGame();
        }
    }
}
