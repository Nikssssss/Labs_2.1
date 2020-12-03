package net;

import protocols.SnakeProto;

import java.net.InetSocketAddress;

public interface MessageHandler {
    void handle(SnakeProto.GameMessage gameMessage, InetSocketAddress sender);
}
