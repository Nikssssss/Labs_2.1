package net;

import models.GameBoardModel;
import models.GameProcess;
import models.UnconfirmedMessage;
import protocols.SnakeProto;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GameBoardMessageHandler implements MessageHandler{
    private GameBoardModel gameBoardModel;
    private UnicastSender unicastSender;
    private GameProcess gameProcess;

    public GameBoardMessageHandler(GameBoardModel gameBoardModel, UnicastSender unicastSender) {
        this.gameBoardModel = gameBoardModel;
        this.unicastSender = unicastSender;
    }

    public void setGameProcess(GameProcess gameProcess){
        this.gameProcess = gameProcess;
    }

    @Override
    public void handle(SnakeProto.GameMessage gameMessage, InetSocketAddress sender) {
        try {
            if (gameMessage.hasPing()) {
                gameBoardModel.updatePlayerTime(sender);
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            } else if (gameMessage.hasSteer()) {
                if (gameBoardModel.addReceivedMessage(gameBoardModel.getPlayerByAddress(sender), gameMessage)) {
                    gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByAddress(sender), gameMessage.getSteer().getDirection());
                }
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            } else if (gameMessage.hasAck()) {
                gameBoardModel.removeUnconfirmedMessage(new UnconfirmedMessage(gameMessage, sender));
                if (gameMessage.hasReceiverId()) {
                    gameBoardModel.setOwnPlayerID(gameMessage.getReceiverId());
                }
            } else if (gameMessage.hasState()) {
                if (gameBoardModel.addReceivedMessage(gameBoardModel.getPlayerByAddress(sender), gameMessage)) {
                    gameProcess.handleMessage(gameMessage, sender);
                }
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            } else if (gameMessage.hasJoin()) {
                gameProcess.handleMessage(gameMessage, sender);
            } else if (gameMessage.hasError()) {
                gameProcess.handleError(gameMessage);
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            } else if (gameMessage.hasRoleChange()) {
                gameProcess.handleMessage(gameMessage, sender);
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
