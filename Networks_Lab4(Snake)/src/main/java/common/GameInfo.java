package common;

import java.net.InetSocketAddress;

public class GameInfo {
    private InetSocketAddress hostAddress;
    private long hostID;
    private int boardWidth;
    private int boardHeight;
    private int foodStatic;
    private float foodPerPerson;
    private boolean isActual;

    public GameInfo(InetSocketAddress hostAddress, long hostID, int boardWidth, int boardHeight, int foodStatic, float foodPerPerson, boolean isActual) {
        this.hostAddress = hostAddress;
        this.hostID = hostID;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.foodStatic = foodStatic;
        this.foodPerPerson = foodPerPerson;
        this.isActual = isActual;
    }

    public InetSocketAddress getHostAddress() {
        return hostAddress;
    }

    public long getHostID() {
        return hostID;
    }

    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }

    public int getFoodStatic() {
        return foodStatic;
    }

    public float getFoodPerPerson() {
        return foodPerPerson;
    }

    public boolean isActual(){
        return isActual;
    }
}
