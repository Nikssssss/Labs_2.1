package net;

import protocols.SnakeProto;

public interface MessageHandler {
    void handle(SnakeProto.GameMessage gameMessage);
}
