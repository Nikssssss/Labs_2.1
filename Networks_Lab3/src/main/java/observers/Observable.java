package observers;

import chat.Message;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Observable {
    protected ArrayList<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer){
        observers.add(observer);
    }

    public void notifyObservers(Message message, InetSocketAddress sender){
        for (var observer : observers){
            observer.update(message, sender);
        }
    }

    public void notifyObservers(Message message, InetSocketAddress sender, InetSocketAddress receiver){
        for (var observer : observers){
            observer.update(message, sender, receiver);
        }
    }
}
