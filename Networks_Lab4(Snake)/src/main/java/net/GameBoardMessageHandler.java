package net;

import models.GameBoardModel;
import gameprocess.GameProcess;
import common.UnconfirmedMessage;
import protocols.SnakeProto.*;

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
    public void handle(GameMessage gameMessage, InetSocketAddress sender) {
        try {
            if (gameMessage.hasPing()) {
                if (gameBoardModel.getDeputyAddress() == null){
                    gameBoardModel.setDeputyAddress(sender);
                    GameMessage sentMessage = unicastSender.sendRoleChangeMsg(null, NodeRole.DEPUTY, sender);
                    gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, sender));
                    gameBoardModel.changePlayerRole(gameBoardModel.getPlayerByAddress(sender), NodeRole.DEPUTY);
                }
                gameBoardModel.updatePlayerTime(gameMessage.getSenderId());
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
                gameBoardModel.setLastSendTime(System.currentTimeMillis());
            } else if (gameMessage.hasSteer()) {
                if (gameBoardModel.addReceivedMessage(gameMessage.getSenderId(), gameMessage)) {
                    if (gameBoardModel.getDeputyAddress() == null) {
                        gameBoardModel.setDeputyAddress(sender);
                        GameMessage sentMessage = unicastSender.sendRoleChangeMsg(null, NodeRole.DEPUTY, sender);
                        gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, sender));
                        gameBoardModel.changePlayerRole(gameBoardModel.getPlayerByAddress(sender), NodeRole.DEPUTY);
                    }
                    gameBoardModel.changeSnakeDirection(gameBoardModel.getPlayerByAddress(sender), gameMessage.getSteer().getDirection());
                }
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
                gameBoardModel.setLastSendTime(System.currentTimeMillis());
                gameBoardModel.updatePlayerTime(gameMessage.getSenderId());
            } else if (gameMessage.hasAck()) {
                gameBoardModel.removeUnconfirmedMessage(gameMessage.getMsgSeq());
                if (gameMessage.hasReceiverId()) {
                    gameBoardModel.setOwnPlayerID(gameMessage.getReceiverId());
                }
                else if (gameMessage.hasSenderId()){
                    gameBoardModel.updatePlayerTime(gameMessage.getSenderId());
                }
            } else if (gameMessage.hasState()) {
                if (gameBoardModel.addReceivedMessage(gameMessage.getSenderId(), gameMessage)) {
                    gameProcess.handleMessage(gameMessage, sender);
                }
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
                gameBoardModel.setLastSendTime(System.currentTimeMillis());
                gameBoardModel.updatePlayerTime(gameMessage.getSenderId());
            } else if (gameMessage.hasJoin()) {
                gameProcess.handleMessage(gameMessage, sender);
            } else if (gameMessage.hasError()) {
                gameProcess.handleError(gameMessage);
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
            } else if (gameMessage.hasRoleChange()) {
                unicastSender.sendAckMsg(gameMessage.getMsgSeq(), sender);
                gameProcess.handleMessage(gameMessage, sender);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
