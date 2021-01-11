package controllers;

import gui.StatusView;
import models.StatusModel;
import protocols.SnakeProto.*;

import javax.swing.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatusController {
    private StatusView statusView;
    private StatusModel statusModel;

    public StatusController(StatusView statusView, StatusModel statusModel){
        this.statusView = statusView;
        this.statusModel = statusModel;
    }

    public JPanel getStatusPanel(){
        return statusView.getStatusPanel();
    }

    public void setHostName(String name){
        statusModel.setHostName(name);
    }

    public void setGameConfig(GameConfig gameConfig){
        statusModel.setGameConfig(gameConfig);
    }

    public void setGameInformation(){
        statusModel.setGameInformation();
    }

    public void updateGameRating(){
        statusModel.updateGameRating();
    }

    public void setPlayersScore(ConcurrentHashMap<GamePlayer, Integer> playersScore){
        statusModel.setPlayersScore(playersScore);
    }
}
