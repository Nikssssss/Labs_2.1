package models;

import gui.StatusView;
import protocols.SnakeProto.*;

import java.util.concurrent.ConcurrentHashMap;

public class StatusModel {
    private ConcurrentHashMap<GamePlayer, Integer> playersScore;
    private final StatusView statusView;
    private GameConfig gameConfig;
    private String hostName;

    public StatusModel(ConcurrentHashMap<GamePlayer, Integer> playersScore, StatusView statusView) {
        this.playersScore = playersScore;
        this.statusView = statusView;
    }

    public void setGameConfig(GameConfig gameConfig){
        this.gameConfig = gameConfig;
    }

    public void setHostName(String hostName){
        this.hostName = hostName;
    }

    public void updateGameRating(){
        statusView.clearRating();
        for (var player : playersScore.entrySet()){
            statusView.addPlayerToRating(player.getKey().getName(), player.getKey().getScore());
        }
    }

    public void setGameInformation(){
        statusView.setGameInformation(hostName, gameConfig.getHeight(), gameConfig.getWidth(), gameConfig.getFoodStatic(), gameConfig.getFoodPerPlayer());
    }

    public void setPlayersScore(ConcurrentHashMap<GamePlayer, Integer> playersScore){
        this.playersScore = playersScore;
    }
}
