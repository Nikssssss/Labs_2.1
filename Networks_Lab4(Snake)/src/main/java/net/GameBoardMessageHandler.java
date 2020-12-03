package net;

import models.GameBoardModel;
import models.UnconfirmedMessage;
import protocols.SnakeProto;

import java.net.InetSocketAddress;

public class GameBoardMessageHandler implements MessageHandler{
    private GameBoardModel gameBoardModel;
    private UnicastSender unicastSender;

    public GameBoardMessageHandler(GameBoardModel gameBoardModel, UnicastSender unicastSender) {
        this.gameBoardModel = gameBoardModel;
        this.unicastSender = unicastSender;
    }

    @Override
    public void handle(SnakeProto.GameMessage gameMessage, InetSocketAddress sender) {
        if (gameMessage.hasPing()){
            gameBoardModel.updatePlayerTime(sender);
            unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
        }
        else if (gameMessage.hasSteer()){
            if (gameBoardModel.addReceivedMessage(gameBoardModel.getPlayerByAddress(sender), gameMessage)) {
                gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByAddress(sender), gameMessage.getSteer().getDirection());
            }
            unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
        }
        else if (gameMessage.hasAck()){
            gameBoardModel.removeUnconfirmedMessage(new UnconfirmedMessage(gameMessage, sender));
            if (gameMessage.hasReceiverId()){
                gameBoardModel.setOwnPlayerID(gameMessage.getReceiverId());
            }
        }
        else if (gameMessage.hasState()){
            if (gameBoardModel.addReceivedMessage(gameBoardModel.getPlayerByAddress(sender), gameMessage)) {

            }
            unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
        }
    }
}
