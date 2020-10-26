package chat;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Message implements Serializable {
    private final MessageType messageType;
    private String textMessage;
    private final UUID uuid;
    private final String clientName;

    public Message(MessageType messageType, UUID uuid, String textMessage, String clientName){
        this.messageType = messageType;
        this.uuid = uuid;
        this.textMessage = textMessage;
        this.clientName = clientName;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getClientName(){
        return clientName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return getTextMessage().equals(message.getTextMessage()) &&
                getUuid().equals(message.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTextMessage(), getUuid());
    }
}
