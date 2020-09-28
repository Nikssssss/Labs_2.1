import java.util.Objects;

public class AppID {
    private final String ip;
    private final Integer port;

    public AppID(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public String getID(){
        return ip + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppID)) return false;
        AppID appID = (AppID) o;
        return ip.equals(appID.ip) &&
                port.equals(appID.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
