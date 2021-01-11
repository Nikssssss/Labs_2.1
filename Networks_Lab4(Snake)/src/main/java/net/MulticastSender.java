package net;

import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;

public class MulticastSender implements Runnable{
    private InetSocketAddress multicastAddress;
    private DatagramSocket unicastSocket;
    private Set<GamePlayer> players;
    private GameConfig config;
    private long currentMsgSeq = Long.MIN_VALUE;

    public MulticastSender(InetSocketAddress multicastAddress, DatagramSocket unicastSocket, Set<GamePlayer> players){
        this.multicastAddress = multicastAddress;
        this.unicastSocket = unicastSocket;
        this.players = players;
    }

    public void setConfig(GameConfig gameConfig){
        this.config = gameConfig;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            sendAnnouncementMsg();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
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
