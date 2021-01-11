package net;

import protocols.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class MulticastReceiver implements Runnable{
    private MulticastSocket multicastSocket;
    private MessageHandler messageHandler;

    public MulticastReceiver(MulticastSocket socket, MessageHandler messageHandler) throws IOException {
        this.multicastSocket = socket;
        this.messageHandler = messageHandler;
        multicastSocket.joinGroup(InetAddress.getByName("239.192.0.4"));
        multicastSocket.setSoTimeout(1000);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet;
        while (!Thread.currentThread().isInterrupted()) {
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                multicastSocket.receive(packet);
                byte[] message = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(message);
                messageHandler.handle(gameMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
            }
            catch (SocketTimeoutException ignored){}
            catch (IOException e) {
                break;
            }
        }
    }
}
