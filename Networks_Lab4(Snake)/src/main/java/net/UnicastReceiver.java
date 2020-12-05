package net;

import models.GameProcess;
import protocols.SnakeProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class UnicastReceiver implements Runnable{
    private DatagramSocket unicastSocket;
    private MessageHandler messageHandler;

    public UnicastReceiver(DatagramSocket unicastSocket, MessageHandler messageHandler) {
        this.unicastSocket = unicastSocket;
        this.messageHandler = messageHandler;
    }

    public void setGameProcess(GameProcess gameProcess){
        messageHandler.setGameProcess(gameProcess);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[5012];
        DatagramPacket packet;
        while (!Thread.currentThread().isInterrupted()) {
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                unicastSocket.receive(packet);
                byte[] message = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(message);
                messageHandler.handle(gameMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
            } catch (IOException e) {
                break;
            }
        }
    }
}
