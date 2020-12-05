package models;

import protocols.SnakeProto.*;

import java.net.InetSocketAddress;

public interface GameProcess{
    void createGame(String name);
    void start();
    void stop();
    void turnLeft();
    void turnRight();
    void turnUp();
    void turnDown();
    void handleMessage(GameMessage gameMessage, InetSocketAddress sender);
    void handleError(GameMessage gameMessage);
}
