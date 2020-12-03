package net;

import models.LobbyModel;
import protocols.SnakeProto.*;

import java.net.InetSocketAddress;

public class LobbyMessageHandler implements MessageHandler{
    private LobbyModel lobbyModel;

    public LobbyMessageHandler(LobbyModel lobbyModel){
        this.lobbyModel = lobbyModel;
    }

    @Override
    public void handle(GameMessage gameMessage, InetSocketAddress sender) {
        if (gameMessage.hasAnnouncement()){
            lobbyModel.addAvailableGame(sender, gameMessage.getAnnouncement().getConfig());
        }
    }
}
