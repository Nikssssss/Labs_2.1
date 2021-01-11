package gameprocess;

import controllers.StatusController;
import gui.GameBoardView;
import launcher.IGameLauncher;
import models.GameBoardModel;
import net.MessageTimeChecker;
import common.UnconfirmedMessage;
import net.PingSender;
import net.UnicastReceiver;
import net.UnicastSender;
import protocols.SnakeProto;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

public class ClientGameProcess implements GameProcess {
    private GameBoardModel gameBoardModel;
    private GameBoardView gameBoardView;
    private UnicastSender unicastSender;
    private UnicastReceiver unicastReceiver;
    private GameConfig gameConfig;
    private Thread unicastReceiverThread;
    private IGameLauncher gameLauncher;
    private MessageTimeChecker messageTimeChecker;
    private Thread messageTimeCheckerThread;
    private PingSender pingSender;
    private Thread pingSenderThread;
    private StatusController statusController;
    private boolean isActive;
    private String name;

    public ClientGameProcess(GameBoardModel gameBoardModel, GameBoardView gameBoardView, UnicastSender unicastSender,
                             UnicastReceiver unicastReceiver, GameConfig gameConfig, InetSocketAddress master, IGameLauncher gameLauncher, StatusController statusController){
        this.gameBoardModel = gameBoardModel;
        this.gameBoardView = gameBoardView;
        this.unicastSender = unicastSender;
        this.unicastReceiver = unicastReceiver;
        this.gameConfig = gameConfig;
        gameBoardModel.setMasterAddress(master);
        unicastReceiverThread = new Thread(unicastReceiver);
        this.gameLauncher = gameLauncher;
        messageTimeChecker = new MessageTimeChecker(gameConfig.getNodeTimeoutMs(), gameConfig.getPingDelayMs(), gameBoardModel.getUnconfirmedMessages(),
                gameBoardModel.getLastMessageTime(), unicastSender, this, gameBoardModel.getReceivedMessages());
        messageTimeCheckerThread = new Thread(messageTimeChecker);
        pingSender = new PingSender(gameConfig.getPingDelayMs(), unicastSender, gameBoardModel.getMasterAddress(), gameBoardModel);
        pingSenderThread = new Thread(pingSender);
        this.statusController = statusController;
        this.isActive = true;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    exit();
                }
            }
        }));
    }

    @Override
    public void createGame(String name) {
        gameBoardModel.createBoard(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setGameBoardSize(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setCells(gameBoardModel.getBoardCells());
        this.name = name;
        statusController.setGameInformation();
    }

    @Override
    public void start() {
        unicastReceiverThread.start();
        try {
            GameMessage sentMessage = unicastSender.sendJoinMessage(name, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        messageTimeCheckerThread.start();
        pingSenderThread.start();
    }

    @Override
    public void stop() {
        unicastReceiverThread.interrupt();
        messageTimeCheckerThread.interrupt();
        pingSenderThread.interrupt();
        gameBoardModel.clearUnconfirmedMessages();
        gameBoardModel.clearLastMessageTime();
        gameBoardModel.clearReceivedMessages();
        try {
            unicastReceiverThread.join();
            messageTimeCheckerThread.join();
            pingSenderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void turnLeft() {
        try {
            GameMessage sentMessage = unicastSender.sendSteerMsg(Direction.LEFT, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void turnRight() {
        try {
            GameMessage sentMessage = unicastSender.sendSteerMsg(Direction.RIGHT, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void turnUp() {
        try {
            GameMessage sentMessage = unicastSender.sendSteerMsg(Direction.UP, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void turnDown() {
        try {
            GameMessage sentMessage = unicastSender.sendSteerMsg(Direction.DOWN, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMessage(GameMessage gameMessage, InetSocketAddress sender) {
        if (gameMessage.hasState()){
            gameBoardModel.clearBoardCells();
            gameBoardModel.clearPlayersScore();
            gameBoardModel.clearReceivedMessages();
            gameBoardModel.clearAllPlayers();
            List<GameState.Snake> snakes = gameMessage.getState().getState().getSnakesList();
            List<GameState.Coord> foods = gameMessage.getState().getState().getFoodsList();
            List<GamePlayer> players = gameMessage.getState().getState().getPlayers().getPlayersList();
            GameConfig config = gameMessage.getState().getState().getConfig();
            for (var snake : snakes){
                if (snake.getPlayerId() == gameBoardModel.getOwnPlayerID()){
                    gameBoardModel.addSnakeCoordinates(getFullCoordinatesFrom(snake.getPointsList()));
                }
                else {
                    gameBoardModel.addEnemySnakeCoordinates(getFullCoordinatesFrom(snake.getPointsList()));
                }
            }
            for (var player : players){
                int playerID = player.getId();
                for (var snake : snakes){
                    if (snake.getPlayerId() == playerID){
                        gameBoardModel.addSnake(player, snake);
                        break;
                    }
                }
                gameBoardModel.addPlayerScore(player);
            }
            gameBoardModel.addAllFoodCoordinates(foods);
            this.gameConfig = config;
            gameBoardView.setCells(gameBoardModel.getBoardCells());
            gameBoardView.repaint();
            statusController.setPlayersScore(gameBoardModel.getPlayersScore());
            statusController.updateGameRating();
        }
        else if (gameMessage.hasRoleChange()){
            GameMessage.RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();
            if (roleChangeMsg.hasReceiverRole()){
                switch (roleChangeMsg.getReceiverRole()){
                    case DEPUTY -> {
                        gameBoardModel.setDeputyState(true);
                    }
                    case MASTER -> {
                        transformToServerProcess(sender);
                    }
                }
            } else if (roleChangeMsg.hasSenderRole()) {
                if (roleChangeMsg.getSenderRole().equals(NodeRole.MASTER)) {
                    if (gameBoardModel.getPlayerByAddress(gameBoardModel.getMasterAddress()) == null){
                        gameBoardModel.removeTimeTrackedPlayer(gameBoardModel.getPlayerByAddress("", 0));
                        gameBoardModel.removeUnconfirmedMessagesBy(gameBoardModel.getPlayerByAddress("", 0));
                    }
                    else {
                        gameBoardModel.removeTimeTrackedPlayer(gameBoardModel.getPlayerByAddress(gameBoardModel.getMasterAddress()));
                        gameBoardModel.removeUnconfirmedMessagesBy(gameBoardModel.getPlayerByAddress(gameBoardModel.getMasterAddress()));
                    }
                    gameBoardModel.setMasterAddress(sender);
                }
            }
            gameBoardModel.setLastSendTime(System.currentTimeMillis());
        }
    }

    @Override
    public void handleError(GameMessage gameMessage) {
        System.out.println(gameMessage.getError().getErrorMessage());
        exit();
        gameLauncher.enterLobby();
    }

    @Override
    public void handlePlayerDeath(GamePlayer gamePlayer) {
        if (gamePlayer.getRole() == NodeRole.MASTER) {
            if (gameBoardModel.isDeputy()) {
                transformToServerProcess(gameBoardModel.getMasterAddress());
            } else {
                for (var player : gameBoardModel.getPlayers()) {
                    if (player.getRole() == NodeRole.DEPUTY) {
                        gameBoardModel.setMasterAddress(new InetSocketAddress(player.getIpAddress(), player.getPort()));
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void exit() {
        isActive = false;
        stop();
        gameBoardModel.clearUnconfirmedMessages();
        try {
            unicastSender.sendRoleChangeMsg(NodeRole.VIEWER, null, gameBoardModel.getMasterAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        gameLauncher.enterLobby();
        gameBoardModel.clearAllPlayers();
        gameBoardModel.clearReceivedMessages();
        gameBoardModel.clearPlayersScore();
        gameBoardModel.clearBoardCells();
        gameBoardModel.clearLastMessageTime();
    }

    public void transformToServerProcess(InetSocketAddress exMaster){
        stop();
        for (var snake : gameBoardModel.getSnakes()){
            gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerBySnake(snake), snake.getHeadDirection());
        }
        GamePlayer gamePlayer = gameBoardModel.getPlayerByAddress(exMaster);
        if (gamePlayer == null){
            gamePlayer = gameBoardModel.getPlayerByAddress("", 0);
        }
        GamePlayer newRolePlayer = gameBoardModel.changePlayerRole(gamePlayer, NodeRole.VIEWER);
        GameState.Snake playerSnake = gameBoardModel.getSnakeByPlayer(newRolePlayer);
        if (playerSnake.getPointsList().size() == 1){
            gameBoardModel.removeSnake(playerSnake);
            gameBoardModel.removeSnakeDirection(newRolePlayer);
            gameBoardModel.removeTimeTrackedPlayer(newRolePlayer);
        }
        else {
            gameBoardModel.transformToZombie(playerSnake);
        }
        for (var entry : gameBoardModel.getPlayerSnakesSet()){
            if (entry.getKey().getId() != gameBoardModel.getOwnPlayerID() && entry.getKey().getRole() != NodeRole.VIEWER){
                try {
                    unicastSender.sendRoleChangeMsg(NodeRole.MASTER, null, new InetSocketAddress(entry.getKey().getIpAddress(), entry.getKey().getPort()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        gameBoardModel.changePlayerRole(gameBoardModel.getPlayerByID(gameBoardModel.getOwnPlayerID()), NodeRole.MASTER);
        gameLauncher.transformClientToServer(gameConfig);
    }

    private List<SnakeProto.GameState.Coord> getFullCoordinatesFrom(List<SnakeProto.GameState.Coord> compressedCoordinates){
        List<SnakeProto.GameState.Coord> fullCoordinates = new LinkedList<>();
        SnakeProto.GameState.Coord lastTurnCoordinates = compressedCoordinates.get(0);
        fullCoordinates.add(lastTurnCoordinates);
        for (int i = 1; i < compressedCoordinates.size(); i++){
            SnakeProto.GameState.Coord currentCoordinates = compressedCoordinates.get(i);
            if (currentCoordinates.getX() == 0){
                if (currentCoordinates.getY() < 0) {
                    for (int j = -1; j >= currentCoordinates.getY(); j--) {
                        int y = lastTurnCoordinates.getY() + j;
                        if (y < 0) y = gameBoardModel.getRows() + y;
                        fullCoordinates.add(coord(lastTurnCoordinates.getX(), y));
                    }
                }
                else {
                    for (int j = 1; j <= currentCoordinates.getY(); j++) {
                        fullCoordinates.add(coord(lastTurnCoordinates.getX(), (lastTurnCoordinates.getY() + j) % gameBoardModel.getRows()));
                    }
                }
            }
            else {
                if (currentCoordinates.getX() < 0) {
                    for (int j = -1; j >= currentCoordinates.getX(); j--) {
                        int x = lastTurnCoordinates.getX() + j;
                        if (x < 0) x = gameBoardModel.getColumns() + x;
                        fullCoordinates.add(coord(x, lastTurnCoordinates.getY()));
                    }
                }
                else {
                    for (int j = 1; j <= currentCoordinates.getX(); j++) {
                        fullCoordinates.add(coord((lastTurnCoordinates.getX() + j) % gameBoardModel.getColumns(), lastTurnCoordinates.getY()));
                    }
                }
            }
            lastTurnCoordinates = fullCoordinates.get(fullCoordinates.size() - 1);
        }
        return fullCoordinates;
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }
}
