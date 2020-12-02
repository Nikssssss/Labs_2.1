package net;

import models.LobbyModel;
import protocols.SnakeProto.*;

public class LobbyMessageHandler implements MessageHandler{
    private LobbyModel lobbyModel;

    public LobbyMessageHandler(LobbyModel lobbyModel){
        this.lobbyModel = lobbyModel;
    }

    @Override
    public void handle(GameMessage gameMessage) {
        if (gameMessage.hasAnnouncement()){

        }
    }
}
