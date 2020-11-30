package chat;

import observers.Observer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Sender implements Observer {
    static Logger logger;
    private final ArrayList<InetSocketAddress> neighborNodes;
    private final DatagramSocket socket;
    private ObjectOutputStream objectOutput;
    private ByteArrayOutputStream byteOutput;
    private final MessageRecorder messageRecorder;
    private final AckChecker ackChecker;

    public Sender(ArrayList<InetSocketAddress> neighborNodes, MessageRecorder messageRecorder, AckChecker ackChecker, DatagramSocket socket) {
        logger = Logger.getLogger(this.getClass().getName());
        this.neighborNodes = neighborNodes;
        this.socket = socket;
        this.messageRecorder = messageRecorder;
        this.ackChecker = ackChecker;
    }

    public synchronized void send(Message message, InetSocketAddress sender) throws IOException {
        DatagramPacket packet;
        byteOutput = new ByteArrayOutputStream();
        objectOutput = new ObjectOutputStream(byteOutput);
        objectOutput.writeObject(message);
        if (message.getMessageType() == MessageType.CONSOLE_REQUEST) {
            for (var neighbor : neighborNodes) {
                packet = new DatagramPacket(byteOutput.toByteArray(), byteOutput.toByteArray().length, neighbor);
                socket.send(packet);
                messageRecorder.addMessage(new MessageID(neighbor, message));
                synchronized (ackChecker) {
                    ackChecker.notifyAll();
                }
            }
        } else if (message.getMessageType() == MessageType.NEIGHBOR_REQUEST || message.getMessageType() == MessageType.SUBSTITUTE_REQUEST) {
            for (var neighbor : neighborNodes) {
                if (!neighbor.equals(sender)) {
                    packet = new DatagramPacket(byteOutput.toByteArray(), byteOutput.toByteArray().length, neighbor);
                    socket.send(packet);
                    messageRecorder.addMessage(new MessageID(neighbor, message));
                    synchronized (ackChecker) {
                        ackChecker.notifyAll();
                    }
                }
            }
        } else if (message.getMessageType() == MessageType.DELETE_REQUEST){
            packet = new DatagramPacket(byteOutput.toByteArray(), byteOutput.toByteArray().length, sender);
            socket.send(packet);
            messageRecorder.addMessage(new MessageID(sender, message));
            synchronized (ackChecker) {
                ackChecker.notifyAll();
            }
        } else {
            packet = new DatagramPacket(byteOutput.toByteArray(), byteOutput.toByteArray().length, sender);
            socket.send(packet);
        }
        logger.info("Sent message with type " + message.getMessageType());
    }

    public void resend(Message message, InetSocketAddress receiver) throws IOException {
        byteOutput = new ByteArrayOutputStream();
        objectOutput = new ObjectOutputStream(byteOutput);
        objectOutput.writeObject(message);
        DatagramPacket packet = new DatagramPacket(byteOutput.toByteArray(), byteOutput.toByteArray().length, receiver);
        socket.send(packet);
    }

    @Override
    public void update(Message message, InetSocketAddress sender) {
        try {
            send(message, sender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Message message, InetSocketAddress sender, InetSocketAddress receiver) {
        try {
            resend(message, receiver);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
