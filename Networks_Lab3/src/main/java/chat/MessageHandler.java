package chat;

import observers.Observable;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class MessageHandler extends Observable {
    static Logger logger;
    private final ArrayList<InetSocketAddress> neighbors;
    private final MessageRecorder messageRecorder;
    private final HashMap<MessageID, Long> receivedMessages;
    private final int deleteTimeout = 60000;

    public MessageHandler(ArrayList<InetSocketAddress> neighbors, MessageRecorder messageRecorder){
        logger = Logger.getLogger(this.getClass().getName());
        this.neighbors = neighbors;
        this.messageRecorder = messageRecorder;
        receivedMessages = new HashMap<>();
    }

    public void handle(Message incomingMessage, DatagramPacket packet){
        Message upcomingMessage;
        if (incomingMessage.getMessageType() == MessageType.ACK){
            messageRecorder.removeMessage(new MessageID(new InetSocketAddress(packet.getAddress(), packet.getPort()), incomingMessage));
        } else if (incomingMessage.getMessageType() == MessageType.SUBSTITUTE_REQUEST){
            designateSubstitute(incomingMessage, packet);
            upcomingMessage = new Message(MessageType.ACK, incomingMessage.getUuid(), incomingMessage.getTextMessage(), incomingMessage.getClientName());
            notifyObservers(upcomingMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
        } else if (incomingMessage.getMessageType() == MessageType.DELETE_REQUEST){
            neighbors.remove(new InetSocketAddress(packet.getAddress(), packet.getPort()));
            upcomingMessage = new Message(MessageType.ACK, incomingMessage.getUuid(), incomingMessage.getTextMessage(), incomingMessage.getClientName());
            notifyObservers(upcomingMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
        } else {
            addReceivedMessage(new MessageID(new InetSocketAddress(packet.getAddress(), packet.getPort()), incomingMessage));
            if (!neighbors.contains(new InetSocketAddress(packet.getAddress(), packet.getPort()))){
                neighbors.add(new InetSocketAddress(packet.getAddress(), packet.getPort()));
            }
            upcomingMessage = new Message(MessageType.ACK, incomingMessage.getUuid(), incomingMessage.getTextMessage(), incomingMessage.getClientName());
            notifyObservers(upcomingMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
            upcomingMessage = new Message(MessageType.NEIGHBOR_REQUEST, incomingMessage.getUuid(), incomingMessage.getTextMessage(), incomingMessage.getClientName());
            notifyObservers(upcomingMessage, new InetSocketAddress(packet.getAddress(), packet.getPort()));
        }
        deleteOldMessages();
    }

    private void addReceivedMessage(MessageID messageID){
        if (!receivedMessages.containsKey(messageID)){
            receivedMessages.put(messageID, System.currentTimeMillis());
            System.out.println(messageID.getMessage().getClientName() + ": " + messageID.getMessage().getTextMessage());
        }
        else {
            receivedMessages.replace(messageID, System.currentTimeMillis());
        }
    }

    private void deleteOldMessages(){
        receivedMessages.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > deleteTimeout);
    }

    private void designateSubstitute(Message substituteMessage, DatagramPacket packet){
        String substituteIP = substituteMessage.getTextMessage().substring(1, substituteMessage.getTextMessage().indexOf(':'));
        int substitutePort = Integer.parseInt(substituteMessage.getTextMessage().substring(substituteMessage.getTextMessage().indexOf(':') + 1));
        neighbors.remove(new InetSocketAddress(packet.getAddress(), packet.getPort()));
        neighbors.add(new InetSocketAddress(substituteIP, substitutePort));
        messageRecorder.removeMessagesSentTo(new InetSocketAddress(packet.getAddress(), packet.getPort()));
    }
}
