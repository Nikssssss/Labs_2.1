package net;

import common.UnconfirmedMessage;
import gameprocess.GameProcess;
import protocols.SnakeProto.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class MessageTimeChecker implements Runnable{
    private final int nodeTimeout;
    private final int pingDelay;
    private final int minimalDelay;
    private ConcurrentHashMap<UnconfirmedMessage, Long> unconfirmedMessages;
    private ConcurrentHashMap<GamePlayer, Long> lastMessageTime;
    private UnicastSender unicastSender;
    private GameProcess gameProcess;
    private ConcurrentHashMap<Integer, GameMessage> receivedMessages;

    public MessageTimeChecker(int nodeTimeout, int pingDelay, ConcurrentHashMap<UnconfirmedMessage, Long> unconfirmedMessages,
                              ConcurrentHashMap<GamePlayer, Long> lastMessageTime, UnicastSender unicastSender, GameProcess gameProcess, ConcurrentHashMap<Integer, GameMessage> receivedMessages) {
        this.nodeTimeout = nodeTimeout;
        this.pingDelay = pingDelay;
        this.unconfirmedMessages = unconfirmedMessages;
        this.lastMessageTime = lastMessageTime;
        this.unicastSender = unicastSender;
        this.gameProcess = gameProcess;
        minimalDelay = Integer.min(pingDelay, nodeTimeout);
        this.receivedMessages = receivedMessages;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            checkNodeDeath();
            checkUnconfirmedMessages();
            receivedMessages.clear();
            try {
                Thread.sleep(minimalDelay);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void checkUnconfirmedMessages(){
        for (var entry : unconfirmedMessages.entrySet()){
            if (entry.getValue() + pingDelay < System.currentTimeMillis()){
                try {
                    unicastSender.sendGameMessage(entry.getKey().getGameMessage(), entry.getKey().getReceiver());
                    unconfirmedMessages.replace(entry.getKey(), System.currentTimeMillis());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void checkNodeDeath(){
        for (var entry : lastMessageTime.entrySet()){
            if (entry.getValue() + nodeTimeout < System.currentTimeMillis()){
                gameProcess.handlePlayerDeath(entry.getKey());
            }
        }
    }
}
