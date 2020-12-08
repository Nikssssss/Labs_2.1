package net;

import gameprocess.GameProcess;
import protocols.SnakeProto.*;

import java.net.InetSocketAddress;

public interface MessageHandler {
    void handle(GameMessage gameMessage, InetSocketAddress sender);
    void setGameProcess(GameProcess gameProcess);
}
