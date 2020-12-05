package launcher;

import controllers.GameBoardController;
import controllers.LobbyController;
import controllers.StatusController;
import gui.GameBoardView;
import gui.LobbyView;
import gui.MainWindow;
import gui.StatusView;
import models.GameBoardModel;
import models.LobbyModel;
import net.*;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

public class GameLauncher implements IGameLauncher{
    private MainWindow mainWindow;
    private GameBoardController gameBoardController;
    private LobbyController lobbyController;
    private StatusController statusController;
    private LobbyView lobbyView;
    private GameBoardView gameBoardView;
    private StatusView statusView;
    private GameBoardModel gameBoardModel;
    private LobbyModel lobbyModel;
    private MulticastSocket multicastSocket;
    private DatagramSocket unicastSocket;
    private MulticastSender multicastSender;
    private UnicastSender unicastSender;
    private MulticastReceiver multicastReceiver;
    private UnicastReceiver unicastReceiver;

    public GameLauncher() throws IOException {
        mainWindow = new MainWindow();
        lobbyView = new LobbyView();
        lobbyModel = new LobbyModel();
        gameBoardModel = new GameBoardModel();
        multicastSocket = new MulticastSocket(9192);
        unicastSocket = new DatagramSocket();
        unicastSender = new UnicastSender(unicastSocket);
        multicastReceiver = new MulticastReceiver(multicastSocket, new LobbyMessageHandler(lobbyModel));
        unicastReceiver = new UnicastReceiver(unicastSocket, new GameBoardMessageHandler(gameBoardModel, unicastSender));
        lobbyController = new LobbyController(lobbyView, lobbyModel, this, multicastReceiver);
        lobbyModel.addObserver(lobbyController);
        lobbyView.addObserver(lobbyController);
        statusView = new StatusView();
        statusController = new StatusController(statusView);
        statusView.addObserver(statusController);
        gameBoardView = new GameBoardView(statusView);
        multicastSender = new MulticastSender(new InetSocketAddress("239.192.0.4", 9192), unicastSocket, gameBoardModel.getPlayers());
        gameBoardController = new GameBoardController(gameBoardView, gameBoardModel, statusController, multicastSender, unicastSender, unicastReceiver);
        gameBoardView.addObserver(gameBoardController);
    }

    @Override
    public void start(){
        mainWindow.showPanel(lobbyController.getLobbyPanel());
    }

    public void createServerGame(GameConfig gameConfig){
        gameBoardController.createServerGame(gameConfig);
        mainWindow.showPanel(gameBoardController.getGameBoardPanel());
    }

    @Override
    public void createClientGame(GameConfig gameConfig, InetSocketAddress master) {
        gameBoardController.createClientGame(gameConfig, master);
        mainWindow.showPanel(gameBoardController.getGameBoardPanel());
    }
}
