package chat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

public class Receiver implements Runnable{
    static Logger logger;
    private final int loss;
    private final DatagramSocket socket;
    private final byte[] buffer;
    private final Random random;
    private final MessageHandler messageHandler;

    public Receiver(int loss, DatagramSocket socket, MessageHandler messageHandler){
        logger = Logger.getLogger(this.getClass().getName());
        this.loss = loss;
        this.socket = socket;
        buffer = new byte[2048];
        random = new Random();
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        Message incomingMessage;
        DatagramPacket packet;
        ObjectInputStream objectInput;
        while (!Thread.currentThread().isInterrupted()){
            packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                objectInput = new ObjectInputStream(new ByteArrayInputStream(buffer));
                incomingMessage = (Message) objectInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                return;
            }
            if (!isLost()){
                logger.info("Received message with type " + incomingMessage.getMessageType() + " from " + packet.getAddress() + ":" + packet.getPort());
                messageHandler.handle(incomingMessage, packet);
            }
        }
    }

    private boolean isLost(){
        return (random.nextInt(99) < loss);
    }
}
