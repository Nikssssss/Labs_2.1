package launcher;

import controllers.GameBoardController;
import controllers.LobbyController;
import controllers.StatusController;
import gui.GameBoardView;
import gui.LobbyView;
import gui.MainWindow;
import gui.StatusView;
import models.GameBoardModel;

public class GameLauncher implements IGameLauncher{
    private MainWindow mainWindow;
    private GameBoardController gameBoardController;
    private LobbyController lobbyController;
    private StatusController statusController;
    private LobbyView lobbyView;
    private GameBoardView gameBoardView;
    private StatusView statusView;
    private GameBoardModel gameBoardModel;

    public GameLauncher(){
        mainWindow = new MainWindow();
        lobbyView = new LobbyView();
        lobbyController = new LobbyController(lobbyView, this);
        lobbyView.addObserver(lobbyController);
        statusView = new StatusView();
        statusController = new StatusController(statusView);
        statusView.addObserver(statusController);
        gameBoardView = new GameBoardView(statusView);
        gameBoardModel = new GameBoardModel();
        gameBoardController = new GameBoardController(gameBoardView, gameBoardModel, statusController);
        gameBoardView.addObserver(gameBoardController);
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
