package models;

public class StateOrderFactory {
    private int currentValue = 0;
    private static volatile StateOrderFactory instance;

    private StateOrderFactory(){}

    public static StateOrderFactory getInstance(){
        StateOrderFactory localInstance = instance;
        if (localInstance == null) {
            synchronized (StateOrderFactory.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new StateOrderFactory();
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
