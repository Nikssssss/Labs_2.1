package gameprocess;

import common.BoardCell;
import common.BoardCellType;
import common.UnconfirmedMessage;
import controllers.StatusController;
import exceptions.IllegalDirectionException;
import exceptions.NoEmptyCellException;
import gui.GameBoardView;
import launcher.IGameLauncher;
import models.*;
import net.MessageTimeChecker;
import net.MulticastSender;
import net.UnicastReceiver;
import net.UnicastSender;
import protocols.SnakeProto;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ServerGameProcess implements GameProcess{
    private Process process;
    private Thread processThread;
    private GameBoardModel gameBoardModel;
    private GameBoardView gameBoardView;
    private Random random = new Random();
    private MulticastSender multicastSender;
    private Thread multicastSenderThread;
    private GameConfig gameConfig;
    private UnicastSender unicastSender;
    private UnicastReceiver unicastReceiver;
    private Thread unicastReceiverThread;
    private IGameLauncher gameLauncher;
    private MessageTimeChecker messageTimeChecker;
    private Thread messageTimeCheckerThread;
    private StatusController statusController;
    private boolean isActive;

    private class Process implements Runnable{
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                gameBoardModel.clearSnakeCells();
                for (var entry : gameBoardModel.getPlayerSnakesSet()){
                    if (entry.getValue().getPointsList().size() > 1) {
                        GameState.Snake prevStepSnake = entry.getValue();
                        GameState.Snake nextStepSnake = moveSnake(prevStepSnake, gameBoardModel.getSnakeDirection(entry.getKey()));
                        gameBoardModel.replaceSnake(prevStepSnake, nextStepSnake);
                    }
                }
                checkCollisions();
                statusController.updateGameRating();
                for (var playerSnakeEntry : gameBoardModel.getPlayerSnakesSet()) {
                    if (playerSnakeEntry.getKey().getRole() != NodeRole.VIEWER) {
                        gameBoardModel.changePlayerScore(playerSnakeEntry.getKey(), gameBoardModel.getPlayerScore(playerSnakeEntry.getKey()));
                    }
                }
                placeFood();
                gameBoardView.setCells(gameBoardModel.getBoardCells());
                gameBoardView.repaint();
                GameState gameState = GameState.newBuilder()
                        .setStateOrder(StateOrderFactory.getInstance().getValue())
                        .addAllSnakes(gameBoardModel.getSnakes())
                        .addAllFoods(gameBoardModel.getAllFood())
                        .setPlayers(GamePlayers.newBuilder().addAllPlayers(gameBoardModel.getPlayers()).build())
                        .setConfig(gameConfig)
                        .build();
                for (var player : gameBoardModel.getPlayerSnakesSet()){
                    if (player.getKey().getId() != gameBoardModel.getOwnPlayerID() && player.getKey().getRole() != NodeRole.VIEWER) {
                        try {
                            InetSocketAddress playerAddress = new InetSocketAddress(player.getKey().getIpAddress(), player.getKey().getPort());
                            GameMessage sentMessage = unicastSender.sendStateMsg(gameState, playerAddress);
                            gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, playerAddress));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(gameConfig.getStateDelayMs());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public ServerGameProcess(GameBoardModel gameBoardModel, GameBoardView gameBoardView, MulticastSender multicastSender,
                             GameConfig config, UnicastSender unicastSender, UnicastReceiver unicastReceiver, IGameLauncher gameLauncher, StatusController statusController) {
        process = new Process();
        this.gameBoardModel = gameBoardModel;
        this.gameBoardView = gameBoardView;
        this.gameConfig = config;
        this.multicastSender = multicastSender;
        multicastSenderThread = new Thread(multicastSender);
        this.unicastSender = unicastSender;
        this.unicastReceiver = unicastReceiver;
        unicastReceiverThread = new Thread(unicastReceiver);
        this.gameLauncher = gameLauncher;
        processThread = new Thread(process);
        messageTimeChecker = new MessageTimeChecker(gameConfig.getNodeTimeoutMs(), gameConfig.getPingDelayMs(), gameBoardModel.getUnconfirmedMessages(),
                gameBoardModel.getLastMessageTime(), unicastSender, this, gameBoardModel.getReceivedMessages());
        messageTimeCheckerThread = new Thread(messageTimeChecker);
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
    public void start(){
        processThread.start();
        multicastSender.setConfig(gameConfig);
        multicastSenderThread.start();
        unicastReceiverThread.start();
        messageTimeCheckerThread.start();
        statusController.setGameInformation();
    }

    @Override
    public void stop(){
        processThread.interrupt();
        multicastSenderThread.interrupt();
        unicastReceiverThread.interrupt();
        messageTimeCheckerThread.interrupt();
    }

    @Override
    public void turnLeft() {
        gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByID(gameBoardModel.getOwnPlayerID()), Direction.LEFT);
    }

    @Override
    public void turnRight() {
        gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByID(gameBoardModel.getOwnPlayerID()), Direction.RIGHT);
    }

    @Override
    public void turnUp() {
        gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByID(gameBoardModel.getOwnPlayerID()), Direction.UP);
    }

    @Override
    public void turnDown() {
        gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByID(gameBoardModel.getOwnPlayerID()), Direction.DOWN);
    }

    @Override
    public void handleMessage(GameMessage gameMessage, InetSocketAddress sender) {
        if (gameMessage.hasJoin()){
            for (var entry : gameBoardModel.getPlayerSnakesSet()){
                if (entry.getKey().getIpAddress().equals(sender.getHostString()) && entry.getKey().getPort() == sender.getPort()
                        && entry.getKey().getRole() != NodeRole.VIEWER){
                    try {
                        unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
            int playerID = PlayerIDFactory.getInstance().getValue();
            NodeRole nodeRole;
            if (gameBoardModel.getNumberOfPlayers() == 1){
                nodeRole = NodeRole.DEPUTY;
            }
            else {
                nodeRole = NodeRole.NORMAL;
            }
            GamePlayer gamePlayer = GamePlayer.newBuilder()
                    .setName(gameMessage.getJoin().getName())
                    .setId(playerID)
                    .setIpAddress(sender.getHostString())
                    .setPort(sender.getPort())
                    .setRole(nodeRole)
                    .setScore(0)
                    .build();
            try {
                GameState.Snake snake = createSnake(playerID);
                try {
                    unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender, playerID);
                    if (nodeRole == NodeRole.DEPUTY){
                        GameMessage sentMessage = unicastSender.sendRoleChangeMsg(null, nodeRole, sender);
                        gameBoardModel.setDeputyAddress(sender);
                        gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, sender));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                gameBoardModel.addSnake(gamePlayer, snake);
                gameBoardModel.changeSnakeDirection(gamePlayer, snake.getHeadDirection());
                gameBoardModel.incrementPlayerScore(gamePlayer);
                gameBoardModel.addTimeTrackedPlayer(gamePlayer);
            } catch (NoEmptyCellException e) {
                try {
                    GameMessage sentMessage = unicastSender.sendErrorMsg("No Empty Cell", sender);
                    gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, sender));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        else if (gameMessage.hasRoleChange()){
            GamePlayer gamePlayer = gameBoardModel.getPlayerByAddress(sender);
            makePlayerViewer(gamePlayer);
        }
    }

    private void makePlayerViewer(GamePlayer gamePlayer){
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
    }

    @Override
    public void handleError(GameMessage gameMessage) {

    }

    @Override
    public void handlePlayerDeath(GamePlayer gamePlayer) {
        switch (gamePlayer.getRole()){
            case DEPUTY -> {
                for (var entry : gameBoardModel.getPlayers()){
                    if (entry.getRole() == NodeRole.NORMAL){
                        try {
                            unicastSender.sendRoleChangeMsg(null, NodeRole.DEPUTY, new InetSocketAddress(entry.getIpAddress(), entry.getPort()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        gameBoardModel.changePlayerRole(entry, NodeRole.DEPUTY);
                        gameBoardModel.setDeputyAddress(new InetSocketAddress(entry.getIpAddress(), entry.getPort()));
                        makePlayerViewer(gamePlayer);
                        gameBoardModel.removeUnconfirmedMessagesBy(gamePlayer);
                        return;
                    }
                }
                makePlayerViewer(gamePlayer);
                gameBoardModel.removeUnconfirmedMessagesBy(gamePlayer);
                gameBoardModel.setDeputyAddress(null);
            }
            case NORMAL -> {
                makePlayerViewer(gamePlayer);
                gameBoardModel.removeUnconfirmedMessagesBy(gamePlayer);
            }
        }
    }

    @Override
    public void exit() {
        isActive = false;
        stop();
        gameBoardModel.clearUnconfirmedMessages();
        if (gameBoardModel.getDeputyAddress() != null) {
            try {
                unicastSender.sendRoleChangeMsg(null, NodeRole.MASTER, gameBoardModel.getDeputyAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        gameLauncher.enterLobby();
        gameBoardModel.clearAllPlayers();
        gameBoardModel.clearReceivedMessages();
        gameBoardModel.clearPlayersScore();
        gameBoardModel.clearBoardCells();
        gameBoardModel.clearLastMessageTime();
    }

    @Override
    public void createGame(String name){
        gameBoardModel.createBoard(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setGameBoardSize(gameConfig.getWidth(), gameConfig.getHeight());
        gameBoardView.setCells(gameBoardModel.getBoardCells());
        int playerID = PlayerIDFactory.getInstance().getValue();
        GameState.Snake snake = null;
        try {
            snake = createSnake(playerID);
        } catch (NoEmptyCellException ignored) {}
        GamePlayer gamePlayer = GamePlayer.newBuilder()
                .setName(name)
                .setId(playerID)
                .setIpAddress("")
                .setPort(0)
                .setRole(NodeRole.MASTER)
                .setScore(0)
                .build();
        gameBoardModel.addSnake(gamePlayer, snake);
        gameBoardModel.changeSnakeDirection(gamePlayer, snake.getHeadDirection());
        gameBoardModel.incrementPlayerScore(gamePlayer);
        placeFood();
        gameBoardModel.setOwnPlayerID(playerID);
    }

    private GameState.Snake createSnake(int playerID) throws NoEmptyCellException {
        GameState.Coord snakeHead = gameBoardModel.getFreeCell();
        Direction headDirection = Direction.forNumber(random.nextInt(4) + 1);
        GameState.Coord snakeBody = null;
        switch (headDirection){
            case UP -> snakeBody = coord(snakeHead.getX(), snakeHead.getY() + 1);
            case DOWN -> snakeBody = coord(snakeHead.getX(), snakeHead.getY() - 1);
            case LEFT -> snakeBody = coord(snakeHead.getX() + 1, snakeHead.getY());
            case RIGHT -> snakeBody = coord(snakeHead.getX() - 1, snakeHead.getY());
        }
        return GameState.Snake.newBuilder()
                .setPlayerId(playerID)
                .setHeadDirection(headDirection)
                .setState(GameState.Snake.SnakeState.ALIVE)
                .addPoints(snakeHead)
                .addPoints(snakeBody)
                .build();
    }

    private GameState.Snake moveSnake(GameState.Snake snake, Direction newHeadDirection){
        List<GameState.Coord> compressedOldCoordinates = snake.getPointsList();
        List<GameState.Coord> fullOldCoordinates = getFullCoordinatesFrom(compressedOldCoordinates);
        GameState.Coord oldHeadCoordinates = snake.getPoints(0);
        GameState.Coord newHeadCoordinates = null;
        try {
            newHeadCoordinates = getNewHeadCoordinates(oldHeadCoordinates, newHeadDirection, snake.getHeadDirection());
        } catch (IllegalDirectionException e) {
            try {
                newHeadCoordinates = getNewHeadCoordinates(oldHeadCoordinates, snake.getHeadDirection(), snake.getHeadDirection());
                newHeadDirection = snake.getHeadDirection();
            } catch (IllegalDirectionException ignored) {}
        }
        List<GameState.Coord> fullNewCoordinates = new LinkedList<>();
        if (!gameBoardModel.isFoodCell(newHeadCoordinates.getX(), newHeadCoordinates.getY())){
            fullOldCoordinates.remove(fullOldCoordinates.size() - 1);
        }
        else {
            gameBoardModel.incrementPlayerScore(gameBoardModel.getPlayerBySnake(snake));
        }
        fullNewCoordinates.add(newHeadCoordinates);
        fullNewCoordinates.addAll(fullOldCoordinates);
        if (gameBoardModel.getOwnPlayerID() == snake.getPlayerId()) {
            gameBoardModel.addSnakeCoordinates(fullNewCoordinates);
        }
        else {
            gameBoardModel.addEnemySnakeCoordinates(fullNewCoordinates);
        }
        return GameState.Snake.newBuilder()
                .setPlayerId(snake.getPlayerId())
                .setHeadDirection(newHeadDirection)
                .setState(snake.getState())
                .addAllPoints(getCompressedCoordinatesFrom(fullNewCoordinates, newHeadDirection))
                .build();
    }

    private GameState.Coord getNewHeadCoordinates(GameState.Coord headCoordinates, Direction newHeadDirection, Direction prevHeadDirection) throws IllegalDirectionException {
        switch(newHeadDirection){
            case UP -> {
                if (prevHeadDirection != Direction.DOWN) {
                    int y = headCoordinates.getY() - 1;
                    if (y < 0) y = gameBoardModel.getRows() + y;
                    return coord(headCoordinates.getX(), y);
                }
            }
            case DOWN -> {
                if (prevHeadDirection != Direction.UP) {
                    int y = headCoordinates.getY() + 1;
                    if (y > gameBoardModel.getRows() - 1) y = y - gameBoardModel.getRows();
                    return coord(headCoordinates.getX(), y);
                }
            }
            case RIGHT -> {
                if (prevHeadDirection != Direction.LEFT) {
                    int x = headCoordinates.getX() + 1;
                    if (x > gameBoardModel.getColumns() - 1) x = x - gameBoardModel.getColumns();
                    return coord(x, headCoordinates.getY());
                }
            }
            case LEFT -> {
                if (prevHeadDirection != Direction.RIGHT) {
                    int x = headCoordinates.getX() - 1;
                    if (x < 0) x = gameBoardModel.getColumns() + x;
                    return coord(x, headCoordinates.getY());
                }
            }
        }
        throw new IllegalDirectionException();
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

    private List<SnakeProto.GameState.Coord> getCompressedCoordinatesFrom(List<SnakeProto.GameState.Coord> fullCoordinates, Direction headDirection){
        List<SnakeProto.GameState.Coord> compressedCoordinates = new LinkedList<>();
        compressedCoordinates.add(fullCoordinates.get(0));
        SnakeProto.GameState.Coord lastTurnCoordinates = fullCoordinates.get(0);
        for (int i = 1; i < fullCoordinates.size(); i++){
            SnakeProto.GameState.Coord currentCoordinates = fullCoordinates.get(i);
            SnakeProto.GameState.Coord nextCoordinates;
            int nextCell;
            int offset;
            if (currentCoordinates.getX() == lastTurnCoordinates.getX()){
                nextCell = i + 1;
                nextCoordinates = currentCoordinates;
                offset = 1;
                while (nextCell < fullCoordinates.size() && fullCoordinates.get(nextCell).getX() == lastTurnCoordinates.getX()){
                    nextCoordinates = fullCoordinates.get(nextCell);
                    nextCell++;
                    offset++;
                }
                int y;
                if (lastTurnCoordinates.getY() == gameBoardModel.getRows() - 1 && currentCoordinates.getY() == 0){
                    y = offset;
                }
                else if (lastTurnCoordinates.getY() == 0 && currentCoordinates.getY() == gameBoardModel.getRows() - 1){
                    y = -offset;
                }
                else if (lastTurnCoordinates.getY() - currentCoordinates.getY() < 0){
                    y = offset;
                }
                else {
                    y = -offset;
                }
                compressedCoordinates.add(coord(0, y));
                i = nextCell - 1;
                lastTurnCoordinates = nextCoordinates;
            }
            else {
                nextCell = i + 1;
                nextCoordinates = currentCoordinates;
                offset = 1;
                while (nextCell < fullCoordinates.size() && fullCoordinates.get(nextCell).getY() == lastTurnCoordinates.getY()){
                    nextCoordinates = fullCoordinates.get(nextCell);
                    nextCell++;
                    offset++;
                }
                int x;
                if (lastTurnCoordinates.getX() == gameBoardModel.getColumns() - 1 && currentCoordinates.getX() == 0){
                    x = offset;
                }
                else if (lastTurnCoordinates.getX() == 0 && currentCoordinates.getX() == gameBoardModel.getColumns() - 1){
                    x = -offset;
                }
                else if (lastTurnCoordinates.getX() - currentCoordinates.getX() < 0){
                    x = offset;
                }
                else {
                    x = -offset;
                }
                compressedCoordinates.add(coord(x, 0));
                i = nextCell - 1;
                lastTurnCoordinates = nextCoordinates;
            }
        }
        return compressedCoordinates;
    }

    private void checkCollisions(){
        List<GameState.Coord> deadSnakesCoordinates = new LinkedList<>();
        List<GameState.Snake> deadSnakes = new LinkedList<>();
        for (var cell : gameBoardModel.getBoardCells()){
            if (cell.getSnakesOnCellCount() > 1){
                if (cell.hasOnlyHeads()) {
                    for (GameState.Snake snake : gameBoardModel.getSnakes()) {
                        if (snake.getPoints(0).equals(coord(cell.getX(), cell.getY()))) {
                            List<GameState.Coord> fullSnakeCoords = getFullCoordinatesFrom(snake.getPointsList());
                            for (int i = 1; i < fullSnakeCoords.size(); i++) {
                                deadSnakesCoordinates.add(fullSnakeCoords.get(i));
                            }
                            gameBoardModel.incrementPlayerScore(gameBoardModel.getPlayerBySnake(snake));
                            deadSnakes.add(snake);
                        }
                    }
                    deadSnakesCoordinates.add(coord(cell.getX(), cell.getY()));
                }
                else {
                    for (GameState.Snake snake : gameBoardModel.getSnakes()) {
                        if (snake.getPoints(0).equals(coord(cell.getX(), cell.getY()))) {
                            List<GameState.Coord> fullSnakeCoords = getFullCoordinatesFrom(snake.getPointsList());
                            for (int i = 1; i < fullSnakeCoords.size(); i++) {
                                deadSnakesCoordinates.add(fullSnakeCoords.get(i));
                            }
                            deadSnakes.add(snake);
                        } else if (getFullCoordinatesFrom(snake.getPointsList()).contains(coord(cell.getX(), cell.getY()))) {
                            gameBoardModel.incrementPlayerScore(gameBoardModel.getPlayerBySnake(snake));
                            BoardCellType boardCellType;
                            if (snake.getPlayerId() == gameBoardModel.getOwnPlayerID()){
                                boardCellType = BoardCellType.OWN_BODY;
                            }
                            else {
                                boardCellType = BoardCellType.ENEMY_BODY;
                            }
                            gameBoardModel.setCellType(coord(cell.getX(), cell.getY()), boardCellType);
                        }
                    }
                }
            }
        }
        for (var coordinate : deadSnakesCoordinates){
            if (random.nextDouble() <= gameConfig.getDeadFoodProb()){
                gameBoardModel.addFoodCoordinates(coordinate);
            }
            else {
                gameBoardModel.emptyCell(coordinate);
            }
        }
        for (var snake : deadSnakes){
            gameBoardModel.addKilledSnake(snake);
        }
        if (deadSnakes.size() > 0) {
            checkViewersSnakes();
            checkGameEnding();
        }
    }

    private void checkViewersSnakes(){
        gameBoardModel.getPlayerSnakesSet().removeIf(playerSnakeEntry -> {
            boolean deleteCondition = playerSnakeEntry.getKey().getRole().equals(NodeRole.VIEWER)
                    && playerSnakeEntry.getValue().getState().equals(GameState.Snake.SnakeState.ZOMBIE)
                    && playerSnakeEntry.getValue().getPointsList().size() == 1;
            if (deleteCondition) {
                gameBoardModel.removeSnakeDirection(playerSnakeEntry.getKey());
                gameBoardModel.removeTimeTrackedPlayer(playerSnakeEntry.getKey());
                gameBoardModel.removeUnconfirmedMessagesBy(playerSnakeEntry.getKey());
            }
            return deleteCondition;
        });
    }

    private void checkGameEnding(){
        if (!gameBoardModel.hasSnakesOnBoard()){
            stop();
            gameLauncher.enterLobby();
        }
    }

    private void placeFood(){
        if (gameBoardModel.getFoodCount() != (gameConfig.getFoodStatic() + gameConfig.getFoodPerPlayer() * gameBoardModel.getAliveSnakesCount())){
            int requiredFood = (gameConfig.getFoodStatic() + (int)gameConfig.getFoodPerPlayer() * gameBoardModel.getAliveSnakesCount()) - gameBoardModel.getFoodCount();
            int emptyCells = gameBoardModel.getEmptyCount();
            if (emptyCells < requiredFood){
                requiredFood = emptyCells;
            }
            ArrayList<BoardCell> cells = gameBoardModel.getBoardCells();
            while (requiredFood > 0){
                BoardCell currentCell = cells.get(random.nextInt(cells.size()));
                if (currentCell.getBoardCellType() == BoardCellType.EMPTY){
                    gameBoardModel.addFoodCoordinates(coord(currentCell.getX(), currentCell.getY()));
                    requiredFood--;
                }
            }
        }
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }
}
