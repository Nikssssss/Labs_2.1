package net;

import models.GameBoardModel;
import common.UnconfirmedMessage;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PingSender implements Runnable{
    private final int pingDelay;
    private UnicastSender unicastSender;
    private InetSocketAddress master;
    private GameBoardModel gameBoardModel;

    public PingSender(int pingDelay, UnicastSender unicastSender, InetSocketAddress master, GameBoardModel gameBoardModel) {
        this.pingDelay = pingDelay;
        this.unicastSender = unicastSender;
        this.master = master;
        this.gameBoardModel = gameBoardModel;
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            if (gameBoardModel.getLastSendTime() + pingDelay < System.currentTimeMillis()){
                try {
                    GameMessage sentMessage = unicastSender.sendPingMessage(master);
                    gameBoardModel.addUnconfirmedMessage(new UnconfirmedMessage(sentMessage, master));
                    gameBoardModel.setLastSendTime(System.currentTimeMillis());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(pingDelay);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
