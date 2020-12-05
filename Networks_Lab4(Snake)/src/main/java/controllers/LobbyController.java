package controllers;

import gui.LobbyView;
import launcher.IGameLauncher;
import models.GameInfo;
import models.LobbyModel;
import net.MulticastReceiver;
import observers.Observer;
import protocols.SnakeProto.*;

import javax.swing.*;

public class LobbyController implements Observer {
    private LobbyView lobbyView;
    private LobbyModel lobbyModel;
    private IGameLauncher gameLauncher;
    private MulticastReceiver multicastReceiver;
    private Thread receiverThread;

    public LobbyController(LobbyView lobbyView, LobbyModel lobbyModel, IGameLauncher gameLauncher, MulticastReceiver multicastReceiver){
        this.lobbyView = lobbyView;
        this.lobbyModel = lobbyModel;
        this.gameLauncher = gameLauncher;
        this.multicastReceiver = multicastReceiver;
        receiverThread = new Thread(multicastReceiver);
        receiverThread.start();
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
        else if (arg instanceof Long){
            GameConfig gameConfig = lobbyModel.getConfigByAddress(lobbyModel.getHostByID((long)arg));
            gameLauncher.createClientGame(gameConfig, lobbyModel.getHostByID((long)arg));
        }
        else if (arg.equals("CreateGame")){
            GameConfig gameConfig = GameConfig.newBuilder()
                    .setWidth(30)
                    .setHeight(30)
                    .setFoodStatic(1)
                    .setFoodPerPlayer(2)
                    .setDeadFoodProb(0.3f)
                    .setNodeTimeoutMs(2000)
                    .build();
            gameLauncher.createServerGame(gameConfig);
        }
    }
}
