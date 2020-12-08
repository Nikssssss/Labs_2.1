package gameprocess;

public class MsgSeqFactory {
    private long currentValue = Long.MIN_VALUE;
    private static volatile MsgSeqFactory instance;

    private MsgSeqFactory(){}

    public static MsgSeqFactory getInstance(){
        MsgSeqFactory localInstance = instance;
        if (localInstance == null) {
            synchronized (MsgSeqFactory.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MsgSeqFactory();
                }
            }
        }
        return localInstance;
    }

    public long getValue(){
        currentValue++;
        return currentValue - 1;
    }
}
