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
import models.StatusModel;
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
    private StatusModel statusModel;
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
        unicastSender = new UnicastSender(unicastSocket, gameBoardModel);
        multicastReceiver = new MulticastReceiver(multicastSocket, new LobbyMessageHandler(lobbyModel));
        unicastReceiver = new UnicastReceiver(unicastSocket, new GameBoardMessageHandler(gameBoardModel, unicastSender));
        lobbyController = new LobbyController(lobbyView, lobbyModel, this, multicastReceiver);
        lobbyModel.addObserver(lobbyController);
        lobbyView.addObserver(lobbyController);
        statusView = new StatusView();
        statusModel = new StatusModel(gameBoardModel.getPlayersScore(), statusView);
        statusController = new StatusController(statusView, statusModel);
        gameBoardView = new GameBoardView(statusView);
        multicastSender = new MulticastSender(new InetSocketAddress("239.192.0.4", 9192), unicastSocket, gameBoardModel.getPlayers());
        gameBoardController = new GameBoardController(gameBoardView, gameBoardModel, statusController,
                multicastSender, unicastSender, unicastReceiver, this);
        gameBoardView.addObserver(gameBoardController);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                unicastSocket.close();
                multicastSocket.close();
            }
        }));
    }

    @Override
    public void start(){
        enterLobby();
    }

    @Override
    public void createServerGame(GameConfig gameConfig){
        gameBoardController.createServerGame(gameConfig);
        mainWindow.showPanel(gameBoardController.getGameBoardPanel());
    }

    @Override
    public void createClientGame(GameConfig gameConfig, InetSocketAddress master) {
        gameBoardController.createClientGame(gameConfig, master);
        mainWindow.showPanel(gameBoardController.getGameBoardPanel());
    }

    @Override
    public void transformClientToServer(GameConfig gameConfig) {
        gameBoardController.transformClientToServer(gameConfig);
    }

    @Override
    public void enterLobby() {
        mainWindow.showPanel(lobbyController.getLobbyPanel());
    }
}
