package controllers;

import gui.StatusView;
import observers.Observer;

import javax.swing.*;

public class StatusController implements Observer {
    private StatusView statusView;

    public StatusController(StatusView statusView){
        this.statusView = statusView;
    }

    @Override
    public void update(Object arg) {

    }

    public JPanel getStatusPanel(){
        return statusView.getStatusPanel();
    }
}
