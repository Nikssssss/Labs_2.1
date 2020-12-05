package models;

import exceptions.NoEmptyCellException;
import protocols.SnakeProto.*;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameBoardModel {
    private int rows;
    private int columns;
    private ArrayList<BoardCell> boardCells;
    private HashMap<GamePlayer, GameState.Snake> playerSnakes;
    private ConcurrentHashMap<GamePlayer, Direction> playerSnakeDirection;
    private HashMap<GamePlayer, Integer> playersScore;
    private HashMap<GamePlayer, Long> lastMessageTime;
    private HashMap<UnconfirmedMessage, Long> unconfirmedMessages;
    private HashMap<GamePlayer, GameMessage> receivedMessages;
    private InetSocketAddress masterAddress;
    private InetSocketAddress deputyAddress;
    private int ownPlayerID;

    public GameBoardModel(){
        boardCells = new ArrayList<>();
        playerSnakes = new HashMap<>();
        playersScore = new HashMap<>();
        playerSnakeDirection = new ConcurrentHashMap<>();
        lastMessageTime = new HashMap<>();
        unconfirmedMessages = new HashMap<>();
        receivedMessages = new HashMap<>();
    }

    public void createBoard(int rows, int columns){
        this.rows = rows;
        this.columns = columns;
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                boardCells.add(new BoardCell(j, i, BoardCellType.EMPTY));
            }
        }
    }

    public void clearSnakeCells(){
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                BoardCell currentBoardCell = boardCells.get(i * rows + j);
                if (!(currentBoardCell.getBoardCellType() == BoardCellType.EMPTY || currentBoardCell.getBoardCellType() == BoardCellType.FOOD)) {
                    currentBoardCell.setBoardCellType(BoardCellType.EMPTY);
                }
            }
        }
    }

    public GameState.Coord getFreeCell() throws NoEmptyCellException {
        for (int i = 0; i < rows; i++){
            for (int j = 0; j < columns; j++){
                BoardCell currentCell = boardCells.get(i * rows + j);
                if (isEmptySurrounding(currentCell)){
                    return coord(currentCell.getX(), currentCell.getY());
                }
            }
        }
        throw new NoEmptyCellException();
    }

    public int getOwnPlayerID(){
        return ownPlayerID;
    }

    private GameState.Coord coord(int x, int y) {
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    private boolean isEmptySurrounding(BoardCell cell){
        for (int k = (cell.getY() - 2); k < (cell.getY() + 2); k++){
            for (int s = (cell.getX() - 2); s < (cell.getX() + 2); s++){
                if (s < 0 || k < 0){
                    if (s < 0) s = columns + s;
                    if (k < 0) k = rows + k;
                }
                if (s > columns || k > rows){
                    if (s > columns) s = s - columns;
                    if (k > rows) k = rows - k;
                }
                if (boardCells.get(k * rows + s).getBoardCellType() != BoardCellType.EMPTY){
                    return false;
                }
            }
        }
        return true;
    }

    public void addSnake(GamePlayer gamePlayer, GameState.Snake snake){
        playerSnakes.put(gamePlayer, snake);
    }

    public void removeSnake(GameState.Snake snake){
        playerSnakes.entrySet().removeIf(playerSnakeEntry -> playerSnakeEntry.getValue().equals(snake));
    }

    public void addSnakeCoordinates(List<GameState.Coord> coordinates){
        BoardCell currentCell = boardCells.get(coordinates.get(0).getY() * rows + coordinates.get(0).getX());
        if (currentCell.getBoardCellType() == BoardCellType.FOOD || currentCell.getBoardCellType() == BoardCellType.EMPTY) {
            currentCell.setBoardCellType(BoardCellType.OWN_HEAD);
        }
        else {
            currentCell.addBoardCellType(BoardCellType.OWN_HEAD);
        }
        for (int i = 1; i < coordinates.size(); i++){
            currentCell = boardCells.get(coordinates.get(i).getY() * rows + coordinates.get(i).getX());
            if (currentCell.getBoardCellType() == BoardCellType.FOOD || currentCell.getBoardCellType() == BoardCellType.EMPTY) {
                currentCell.setBoardCellType(BoardCellType.OWN_BODY);
            }
            else{
                currentCell.addBoardCellType(BoardCellType.OWN_BODY);
            }
        }
    }

    public void addEnemySnakeCoordinates(List<GameState.Coord> coordinates){
        BoardCell currentCell = boardCells.get(coordinates.get(0).getY() * rows + coordinates.get(0).getX());
        if (currentCell.getBoardCellType() == BoardCellType.FOOD || currentCell.getBoardCellType() == BoardCellType.EMPTY) {
            currentCell.setBoardCellType(BoardCellType.ENEMY_HEAD);
        }
        else {
            currentCell.addBoardCellType(BoardCellType.ENEMY_HEAD);
        }
        for (int i = 1; i < coordinates.size(); i++){
            currentCell = boardCells.get(coordinates.get(i).getY() * rows + coordinates.get(i).getX());
            if (currentCell.getBoardCellType() == BoardCellType.FOOD || currentCell.getBoardCellType() == BoardCellType.EMPTY) {
                currentCell.setBoardCellType(BoardCellType.ENEMY_BODY);
            }
            else{
                currentCell.addBoardCellType(BoardCellType.ENEMY_BODY);
            }
        }
    }

    public void setCellType(GameState.Coord cell, BoardCellType boardCellType){
        boardCells.get(cell.getY() * rows + cell.getX()).setBoardCellType(boardCellType);
    }

    public void addUnconfirmedMessage(UnconfirmedMessage unconfirmedMessage){
        unconfirmedMessages.put(unconfirmedMessage, System.currentTimeMillis());
    }

    public void removeUnconfirmedMessage(UnconfirmedMessage unconfirmedMessage){
        unconfirmedMessages.remove(unconfirmedMessage);
    }

    public void updateUnconfirmedMessage(UnconfirmedMessage unconfirmedMessage){
        unconfirmedMessages.replace(unconfirmedMessage, System.currentTimeMillis());
    }

    public boolean addReceivedMessage(GamePlayer gamePlayer, GameMessage gameMessage){
        for (var entry : receivedMessages.entrySet()){
            if (entry.getKey().equals(gamePlayer)){
                GameMessage currentMessage = entry.getValue();
                if (currentMessage.hasSteer() && gameMessage.hasSteer()){
                    if (currentMessage.getMsgSeq() < gameMessage.getMsgSeq()){
                        receivedMessages.replace(gamePlayer, gameMessage);
                        return true;
                    }
                    else return false;
                }
                else if (currentMessage.hasState() && gameMessage.hasState()){
                    if (currentMessage.getMsgSeq() < gameMessage.getMsgSeq()){
                        receivedMessages.replace(gamePlayer, gameMessage);
                        return true;
                    }
                    else return false;
                }
            }
        }
        receivedMessages.put(gamePlayer, gameMessage);
        return true;
    }

    public void addAllFoodCoordinates(List<GameState.Coord> coordinates){
        for (var coordinate : coordinates){
            boardCells.get(coordinate.getY() * rows + coordinate.getX()).setBoardCellType(BoardCellType.FOOD);
        }
    }

    public void setMasterAddress(InetSocketAddress masterAddress){
        this.masterAddress = masterAddress;
    }

    public void changePlayerRole(GamePlayer gamePlayer, NodeRole nodeRole){
        GamePlayer changingPlayer = null;
        GameState.Snake playerSnake = null;
        for (var entry : playerSnakes.entrySet()){
            if (entry.getKey().equals(gamePlayer)){
                changingPlayer = entry.getKey();
                playerSnake = entry.getValue();
            }
        }
        playerSnakes.remove(changingPlayer);
        playersScore.remove(changingPlayer);
        Direction snakeDirection = playerSnakeDirection.get(changingPlayer);
        playerSnakeDirection.remove(changingPlayer);
        lastMessageTime.remove(changingPlayer);
        GamePlayer newRolePlayer = GamePlayer.newBuilder()
                .setName(changingPlayer.getName())
                .setId(changingPlayer.getId())
                .setIpAddress(changingPlayer.getIpAddress())
                .setPort(changingPlayer.getPort())
                .setRole(nodeRole)
                .setScore(changingPlayer.getScore())
                .build();
        playerSnakes.put(newRolePlayer, playerSnake);
        playersScore.put(newRolePlayer, newRolePlayer.getScore());
        playerSnakeDirection.put(newRolePlayer, snakeDirection);
    }

    public void updatePlayerTime(InetSocketAddress playerAddress){
        for (var entry : lastMessageTime.entrySet()){
            if (entry.getKey().getIpAddress().equals(playerAddress.getHostName()) && entry.getKey().getPort() == playerAddress.getPort()){
                lastMessageTime.replace(entry.getKey(), System.currentTimeMillis());
                break;
            }
        }
    }

    public void addTimeTrackedPlayer(GamePlayer gamePlayer){
        lastMessageTime.put(gamePlayer, System.currentTimeMillis());
    }

    public void addFoodCoordinates(GameState.Coord coordinates){
        boardCells.get(coordinates.getY() * rows + coordinates.getX()).setBoardCellType(BoardCellType.FOOD);
    }

    public void emptyCell(GameState.Coord coordinates){
        boardCells.get(coordinates.getY() * rows + coordinates.getX()).setBoardCellType(BoardCellType.EMPTY);
    }

    public Set<GamePlayer> getPlayers(){
        return playersScore.keySet();
    }

    public int getFoodCount(){
        int count = 0;
        for (var cell : boardCells){
            if (cell.getBoardCellType() == BoardCellType.FOOD){
                count++;
            }
        }
        return count;
    }

    public void setOwnPlayerID(int ownPlayerID){
        this.ownPlayerID = ownPlayerID;
    }

    public ArrayList<BoardCell> getBoardCells(){
        return boardCells;
    }

    public ArrayList<GameState.Snake> getSnakes(){
        return new ArrayList<>(playerSnakes.values());
    }

    public int getRows(){
        return rows;
    }

    public int getColumns(){
        return columns;
    }

    public boolean isFoodCell(int x, int y){
        return boardCells.get(rows * y + x).getBoardCellType() == BoardCellType.FOOD;
    }

    public boolean isEmptyCell(int x, int y){
        return boardCells.get(rows * y + x).getBoardCellType() == BoardCellType.EMPTY;
    }

    public List<GameState.Coord> getAllFood(){
        List<GameState.Coord> allFood = new ArrayList<>();
        for (var cell : boardCells){
            if (cell.getBoardCellType() == BoardCellType.FOOD){
                allFood.add(coord(cell.getX(), cell.getY()));
            }
        }
        return allFood;
    }

    public int getEmptyCount(){
        int count = 0;
        for (var cell : boardCells){
            if (cell.getBoardCellType() == BoardCellType.EMPTY){
                count++;
            }
        }
        return count;
    }

    public Set<Map.Entry<GamePlayer, GameState.Snake>> getPlayerSnakesSet(){
        return playerSnakes.entrySet();
    }

    public GamePlayer getPlayerByID(int playerID){
        GamePlayer gamePlayer = null;
        for (var entry : playerSnakes.entrySet()){
            if (entry.getKey().getId() == playerID){
                gamePlayer = entry.getKey();
                break;
            }
        }
        return gamePlayer;
    }

    public GamePlayer getPlayerByAddress(InetSocketAddress playerAddress){
        GamePlayer gamePlayer = null;
        for (var entry : playerSnakes.entrySet()){
            if (entry.getKey().getIpAddress().equals(playerAddress.getHostString()) && entry.getKey().getPort() == playerAddress.getPort()){
                gamePlayer = entry.getKey();
                break;
            }
        }
        return gamePlayer;
    }

    public void changeSnakeDirection(GamePlayer gamePlayer, Direction headDirection){
        if (playerSnakeDirection.containsKey(gamePlayer)){
            playerSnakeDirection.replace(gamePlayer, headDirection);
        }
        else {
            playerSnakeDirection.put(gamePlayer, headDirection);
        }
    }

    public Direction getSnakeDirection(GamePlayer gamePlayer) {
        return playerSnakeDirection.get(gamePlayer);
    }

    public void incrementPlayerScore(GamePlayer gamePlayer){
        if (!playersScore.containsKey(gamePlayer)){
            playersScore.put(gamePlayer, 0);
        }
        else {
            int oldScore = playersScore.get(gamePlayer);
            playersScore.replace(gamePlayer, oldScore + 1);
        }
    }

    public void addAllPlayersScore(List<GamePlayer> gamePlayers){
        for (var gamePlayer : gamePlayers){
            playersScore.put(gamePlayer, gamePlayer.getScore());
        }
    }

    public void replaceSnake(GameState.Snake oldSnake, GameState.Snake newSnake){
        for (var entry : playerSnakes.entrySet()){
            if (entry.getValue().equals(oldSnake)){
                playerSnakes.replace(entry.getKey(), newSnake);
                break;
            }
        }
    }

    public void clearBoardCells(){
        for (var cell : boardCells){
            cell.setBoardCellType(BoardCellType.EMPTY);
        }
    }

    public void clearPlayersScore(){
        playersScore.clear();
    }

    public void clearReceivedMessages(){
        receivedMessages.clear();
    }

    public InetSocketAddress getMasterAddress(){
        return masterAddress;
    }

    public int getNumberOfPlayers(){
        return playerSnakes.size();
    }
}
