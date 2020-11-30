package observers;

import chat.Message;

import java.net.InetSocketAddress;

public interface Observer {
    void update(Message message, InetSocketAddress sender);
    void update(Message message, InetSocketAddress sender, InetSocketAddress receiver);
}
