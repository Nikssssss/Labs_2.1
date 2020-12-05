package models;

public class PlayerIDFactory {
    private int currentValue = 0;
    private static volatile PlayerIDFactory instance;

    private PlayerIDFactory(){}

    public static PlayerIDFactory getInstance(){
        PlayerIDFactory localInstance = instance;
        if (localInstance == null) {
            synchronized (PlayerIDFactory.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new PlayerIDFactory();
                }
            }
        }
        return localInstance;
    }

    public int getValue(){
        currentValue++;
        return currentValue - 1;
    }
}
