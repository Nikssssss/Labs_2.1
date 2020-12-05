package models;

import observers.Observable;
import protocols.SnakeProto.*;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class LobbyModel extends Observable {
    private HashMap<InetSocketAddress, GameConfig> availableGames;
    private HashMap<InetSocketAddress, Long> hostsID;

    public LobbyModel(){
        availableGames = new HashMap<>();
        hostsID = new HashMap<>();
    }

    public void addAvailableGame(InetSocketAddress hostAddress, GameConfig gameConfig){
        if (availableGames.containsKey(hostAddress)){
            if (!availableGames.get(hostAddress).equals(gameConfig)){
                availableGames.replace(hostAddress, gameConfig);
                notifyObservers(new GameInfo(hostAddress, hostsID.get(hostAddress), gameConfig.getWidth(),
                        gameConfig.getHeight(), gameConfig.getFoodStatic(), gameConfig.getFoodPerPlayer(), true));
            }
        }
        else {
            availableGames.put(hostAddress, gameConfig);
            long hostID = 0;
            while (hostsID.containsValue(hostID)){
                hostID++;
            }
            hostsID.put(hostAddress, hostID);
            notifyObservers(new GameInfo(hostAddress, hostsID.get(hostAddress), gameConfig.getWidth(),
                    gameConfig.getHeight(), gameConfig.getFoodStatic(), gameConfig.getFoodPerPlayer(), true));
        }
    }

    public InetSocketAddress getHostByID(long hostID){
        InetSocketAddress hostAddress = null;
        for (var entry : hostsID.entrySet()){
            if (entry.getValue().equals(hostID)){
                hostAddress = entry.getKey();
                break;
            }
        }
        return hostAddress;
    }

    public GameConfig getConfigByAddress(InetSocketAddress socketAddress){
        return availableGames.get(socketAddress);
    }

    public void removeAvailableGame(InetSocketAddress hostAddress){
        notifyObservers(new GameInfo(hostAddress, hostsID.get(hostAddress), availableGames.get(hostAddress).getWidth(),
                availableGames.get(hostAddress).getHeight(), availableGames.get(hostAddress).getFoodStatic(),
                availableGames.get(hostAddress).getFoodPerPlayer(), false));
        availableGames.remove(hostAddress);
        hostsID.remove(hostAddress);
    }

    public HashMap<InetSocketAddress, GameConfig> getAvailableGames(){
        return availableGames;
    }

    public HashMap<InetSocketAddress, Long> getHostsID(){
        return hostsID;
    }
}
