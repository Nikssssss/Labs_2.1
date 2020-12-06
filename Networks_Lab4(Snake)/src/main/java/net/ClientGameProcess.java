package net;

import gui.GameBoardView;
import launcher.IGameLauncher;
import models.GameBoardModel;
import models.GameProcess;
import models.UnconfirmedMessage;
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

    public ClientGameProcess(GameBoardModel gameBoardModel, GameBoardView gameBoardView, UnicastSender unicastSender,
                             UnicastReceiver unicastReceiver, GameConfig gameConfig, InetSocketAddress master, IGameLauncher gameLauncher){
        this.gameBoardModel = gameBoardModel;
        this.gameBoardView = gameBoardView;
        this.unicastSender = unicastSender;
        this.unicastReceiver = unicastReceiver;
        this.gameConfig = gameConfig;
        gameBoardModel.setMasterAddress(master);
        unicastReceiverThread = new Thread(unicastReceiver);
        this.gameLauncher = gameLauncher;
    }

    @Override
    public void createGame(String name) {
        gameBoardModel.createBoard(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setGameBoardSize(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setCells(gameBoardModel.getBoardCells());
    }

    @Override
    public void start() {
        unicastReceiverThread.start();
        try {
            GameMessage sentMessage = unicastSender.sendJoinMessage("name", gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        unicastReceiverThread.interrupt();
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
            gameBoardModel.addAllFoodCoordinates(foods);
            gameBoardModel.addAllPlayersScore(players);
            this.gameConfig = config;
            gameBoardView.setCells(gameBoardModel.getBoardCells());
            gameBoardView.repaint();
        }
    }

    @Override
    public void handleError(GameMessage gameMessage) {
        System.out.println(gameMessage.getError().getErrorMessage());
    }

    @Override
    public void exit() {
        try {
            GameMessage sentMessage = unicastSender.sendRoleChangeMsg(NodeRole.VIEWER, null, gameBoardModel.getMasterAddress());
            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, gameBoardModel.getMasterAddress()));
            while (gameBoardModel.hasUnconfirmedMessages()){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
            stop();
            gameLauncher.enterLobby();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
