package chat;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRecorder {
    private final ConcurrentHashMap<MessageID, Long> unconfirmedMessages;

    public MessageRecorder() {
        unconfirmedMessages = new ConcurrentHashMap<>();
    }

    public void addMessage(MessageID messageID){
        unconfirmedMessages.put(messageID, System.currentTimeMillis());
    }

    public void removeMessage(MessageID messageID){
        unconfirmedMessages.remove(messageID);
    }

    public boolean isEmpty(){
        return unconfirmedMessages.isEmpty();
    }

    public ConcurrentHashMap<MessageID, Long> getOverdueMessages(int timeout){
        ConcurrentHashMap<MessageID, Long> overdueMessages = new ConcurrentHashMap<>();
        for (Map.Entry<MessageID, Long> entry : unconfirmedMessages.entrySet()){
            if (System.currentTimeMillis() - entry.getValue() > timeout){
                overdueMessages.put(entry.getKey(), entry.getValue());
            }
        }
        return overdueMessages;
    }

    public boolean hasMessageType(MessageType messageType){
        for (Map.Entry<MessageID, Long> entry : unconfirmedMessages.entrySet()){
            if (entry.getKey().getMessage().getMessageType() == messageType){
                return true;
            }
        }
        return false;
    }

    public void removeMessagesSentTo(InetSocketAddress address){
        unconfirmedMessages.entrySet().removeIf(entry -> entry.getKey().getAddress().equals(address));
    }
}
