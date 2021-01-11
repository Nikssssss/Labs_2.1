import launcher.GameLauncher;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        GameLauncher gameLauncher;
        try {
            gameLauncher = new GameLauncher();
            gameLauncher.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
