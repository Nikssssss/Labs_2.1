package net;

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

    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet;
        while (!Thread.currentThread().isInterrupted()) {
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                unicastSocket.receive(packet);
                buffer = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(buffer);
                messageHandler.handle(gameMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
            } catch (IOException e) {
                break;
            }
        }
    }
}
