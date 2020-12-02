package net;

import protocols.SnakeProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.Arrays;

public class Receiver implements Runnable{
    private MulticastSocket socket;
    private MessageHandler messageHandler;

    public Receiver(DatagramSocket socket, MessageHandler messageHandler){
        this.socket = (MulticastSocket) socket;
        this.messageHandler = messageHandler;
    }

    public Receiver(MulticastSocket socket, MessageHandler messageHandler){
        this.socket = socket;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet;
        while (!Thread.currentThread().isInterrupted()) {
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                buffer = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(buffer);
                messageHandler.handle(gameMessage);
            } catch (IOException e) {
                break;
            }
        }
    }
}
