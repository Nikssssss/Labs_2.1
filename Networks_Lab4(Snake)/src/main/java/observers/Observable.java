package observers;

import java.util.ArrayList;

public class Observable {
    private ArrayList<Observer> observers;

    public void addObserver(Observer observer){
        if (observers == null){
            observers = new ArrayList<>();
        }
        observers.add(observer);
    }

    public void removeObserver(Observer observer){
        observers.remove(observer);
    }

    public void notifyObservers(Object arg){
        for (var observer: observers){
            observer.update(arg);
        }
    }
}
