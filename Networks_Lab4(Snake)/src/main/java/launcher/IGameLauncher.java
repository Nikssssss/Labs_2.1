package launcher;

import protocols.SnakeProto.*;

import java.net.InetSocketAddress;

public interface IGameLauncher {
    void start();
    void createServerGame(GameConfig gameConfig);
    void createClientGame(GameConfig gameConfig, InetSocketAddress master);
}
