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

    public GameMessage sendJoinMessage(String name, InetSocketAddress receiverAddress) throws IOException {
        GameMessage.JoinMsg joinMsg = GameMessage.JoinMsg.newBuilder()
                .setName(name)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setJoin(joinMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
        return gameMessage;
    }

    public GameMessage sendPingMessage(InetSocketAddress receiverAddress) throws IOException {
        GameMessage.PingMsg pingMsg = GameMessage.PingMsg.newBuilder()
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setPing(pingMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
        return gameMessage;
    }

    public GameMessage sendSteerMsg(Direction direction, InetSocketAddress receiverAddress) throws IOException {
        GameMessage.SteerMsg steerMsg = GameMessage.SteerMsg.newBuilder()
                .setDirection(direction)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setSteer(steerMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
        return gameMessage;
    }

    public void sendAckMsg(long receivedMsgSeq, InetSocketAddress receiverAddress) throws IOException {
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder()
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(receivedMsgSeq)
                .setAck(ackMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public void sendAckMsg(long receivedMsgSeq, InetSocketAddress receiverAddress, int receiverID) throws IOException {
        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder()
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(receivedMsgSeq)
                .setReceiverId(receiverID)
                .setAck(ackMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
    }

    public GameMessage sendStateMsg(GameState gameState, InetSocketAddress receiverAddress) throws IOException {
        GameMessage.StateMsg stateMsg = GameMessage.StateMsg.newBuilder()
                .setState(gameState)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setState(stateMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
        return gameMessage;
    }

    public GameMessage sendErrorMsg(String errorMessage, InetSocketAddress receiverAddress) throws IOException {
        GameMessage.ErrorMsg errorMsg = GameMessage.ErrorMsg.newBuilder()
                .setErrorMessage(errorMessage)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(MsgSeqFactory.getInstance().getValue())
                .setError(errorMsg)
                .build();
        sendGameMessage(gameMessage, receiverAddress);
        return gameMessage;
    }

    public GameMessage sendRoleChangeMsg(NodeRole senderRole, NodeRole receiverRole, InetSocketAddress receiverAddress) throws IOException {
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
        return gameMessage;
    }

    private void sendGameMessage(GameMessage gameMessage, InetSocketAddress receiverAddress) throws IOException {
        byte[] byteMessage = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, receiverAddress);
        unicastSocket.send(packet);
    }
}
