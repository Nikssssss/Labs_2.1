package models;

import protocols.SnakeProto.*;

import java.net.InetSocketAddress;
import java.util.Objects;

public class UnconfirmedMessage {
    private GameMessage gameMessage;
    private InetSocketAddress receiver;

    public UnconfirmedMessage(GameMessage gameMessage, InetSocketAddress receiver) {
        this.gameMessage = gameMessage;
        this.receiver = receiver;
    }

    public GameMessage getGameMessage() {
        return gameMessage;
    }

    public InetSocketAddress getReceiver() {
        return receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnconfirmedMessage)) return false;
        UnconfirmedMessage that = (UnconfirmedMessage) o;
        return getGameMessage().equals(that.getGameMessage()) &&
                getReceiver().equals(that.getReceiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGameMessage(), getReceiver());
    }
}
