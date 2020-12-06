package net;

import models.GameProcess;
import protocols.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UnicastReceiver implements Runnable{
    private DatagramSocket unicastSocket;
    private MessageHandler messageHandler;

    public UnicastReceiver(DatagramSocket unicastSocket, MessageHandler messageHandler) {
        this.unicastSocket = unicastSocket;
        this.messageHandler = messageHandler;
        try {
            unicastSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
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
            } catch (SocketTimeoutException ignored){ }
            catch (IOException e) {
                break;
            }
        }
    }
}
