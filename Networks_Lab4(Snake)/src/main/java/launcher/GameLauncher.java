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

import java.io.IOException;
import java.net.DatagramSocket;
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

    public GameLauncher() throws IOException {
        mainWindow = new MainWindow();
        lobbyView = new LobbyView();
        lobbyModel = new LobbyModel();
        lobbyController = new LobbyController(lobbyView, lobbyModel, this);
        lobbyModel.addObserver(lobbyController);
        lobbyView.addObserver(lobbyController);
        statusView = new StatusView();
        statusController = new StatusController(statusView);
        statusView.addObserver(statusController);
        gameBoardView = new GameBoardView(statusView);
        gameBoardModel = new GameBoardModel();
        gameBoardController = new GameBoardController(gameBoardView, gameBoardModel, statusController);
        gameBoardView.addObserver(gameBoardController);
        multicastSocket = new MulticastSocket(9192);
        unicastSocket = new DatagramSocket();
    }

    @Override
    public void start(){
        mainWindow.showPanel(lobbyController.getLobbyPanel());
    }

    public void createGame(){
        gameBoardController.createGame();
        mainWindow.showPanel(gameBoardController.getGameBoardPanel());
    }
}
