package net;

import models.MsgSeqFactory;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UnicastSender {
    private DatagramSocket unicastSocket;

    public UnicastSender(DatagramSocket unicastSocket){
        this.unicastSocket = unicastSocket;
    }

    public void sendJoinMessage(String name, InetSocketAddress receiverAddress){
        GameMessage.JoinMsg joinMsg = GameMessage.JoinMsg.newBuilder()
                .setName(name)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setJoin(joinMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendPingMessage(InetSocketAddress receiverAddress){
        GameMessage.PingMsg pingMsg = GameMessage.PingMsg.newBuilder()
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setPing(pingMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendSteerMsg(Direction direction, InetSocketAddress receiverAddress){
        GameMessage.SteerMsg steerMsg = GameMessage.SteerMsg.newBuilder()
                .setDirection(direction)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setSteer(steerMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendAckMsg(long receivedMsgSeq, InetSocketAddress receiverAddress){
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder()
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(receivedMsgSeq)
                .setAck(ackMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendStateMsg(GameState gameState, InetSocketAddress receiverAddress){
        GameMessage.StateMsg stateMsg = GameMessage.StateMsg.newBuilder()
                .setState(gameState)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setState(stateMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendErrorMsg(String errorMessage, InetSocketAddress receiverAddress){
        GameMessage.ErrorMsg errorMsg = GameMessage.ErrorMsg.newBuilder()
                .setErrorMessage(errorMessage)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setError(errorMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendRoleChangeMsg(NodeRole senderRole, NodeRole receiverRole, InetSocketAddress receiverAddress){
        GameMessage.RoleChangeMsg roleChangeMsg;
        if (receiverRole == null) {
            roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                    .setSenderRole(senderRole)
                    .build();
        }
        else {
            roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                    .setReceiverRole(receiverRole)
                    .build();
        }
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setRoleChange(roleChangeMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    private void sendGameMessage(GameMessage gameMessage, InetSocketAddress receiverAddress){
        byte[] byteMessage = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, receiverAddress);
        try {
            unicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
