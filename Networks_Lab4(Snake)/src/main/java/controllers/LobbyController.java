package controllers;

import gui.LobbyView;
import launcher.IGameLauncher;
import observers.Observer;

import javax.swing.*;

public class LobbyController implements Observer {
    private LobbyView lobbyView;
    private IGameLauncher gameLauncher;

    public LobbyController(LobbyView lobbyView, IGameLauncher gameLauncher){
        this.lobbyView = lobbyView;
        this.gameLauncher = gameLauncher;
    }

    public JPanel getLobbyPanel() {
        return lobbyView.getLobbyPanel();
    }

    @Override
    public void update(Object arg) {
        if (arg.equals("CreateGame")){
            gameLauncher.createGame();
        }
    }
}
