package launcher;

import protocols.SnakeProto.*;

public interface IGameLauncher {
    void start();
    void createGame(GameConfig gameConfig);
}
