package chat;

import java.net.InetSocketAddress;
import java.util.Objects;

public class MessageID {
    private InetSocketAddress address;
    private Message message;

    public MessageID(InetSocketAddress address, Message message) {
        this.address = address;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageID)) return false;
        MessageID that = (MessageID) o;
        return Objects.equals(address, that.address) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, message);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public Message getMessage() {
        return message;
    }
}
