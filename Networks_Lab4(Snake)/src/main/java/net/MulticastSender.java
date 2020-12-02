package net;

import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class MulticastSender implements Runnable{
    private InetSocketAddress multicastAddress;
    private DatagramSocket unicastSocket;
    private ArrayList<GamePlayer> players;
    private final GameConfig config;
    private long currentMsgSeq = Long.MIN_VALUE;

    public MulticastSender(InetSocketAddress multicastAddress, DatagramSocket unicastSocket, ArrayList<GamePlayer> players, GameConfig config){
        this.multicastAddress = multicastAddress;
        this.unicastSocket = unicastSocket;
        this.players = players;
        this.config = config;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            sendAnnouncementMsg();
        }
    }

    private void sendAnnouncementMsg(){
        GamePlayers gamePlayers = GamePlayers.newBuilder()
                .addAllPlayers(players)
                .build();
        GameMessage.AnnouncementMsg announcementMsg = GameMessage.AnnouncementMsg.newBuilder()
                .setPlayers(gamePlayers)
                .setConfig(config)
                .build();
        GameMessage gameMessage = GameMessage.newBuilder()
                .setMsgSeq(currentMsgSeq)
                .setAnnouncement(announcementMsg)
                .build();
        byte[] byteMessage = gameMessage.toByteArray();
        DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, multicastAddress);
        try {
            unicastSocket.send(packet);
            currentMsgSeq++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
