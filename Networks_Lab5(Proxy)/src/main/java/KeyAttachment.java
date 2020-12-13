import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class KeyAttachment {
    private ByteBuffer input;
    private ByteBuffer output;
    private ClientMessageStatus clientMessageStatus;
    private SelectionKey remoteServer;

    public KeyAttachment(ByteBuffer input, ByteBuffer output){
        clientMessageStatus = ClientMessageStatus.GREETING;
        this.input = input;
        this.output = output;
    }

    public ByteBuffer getInput() {
        return input;
    }

    public ByteBuffer getOutput() {
        return output;
    }

    public ClientMessageStatus getClientMessageStatus() {
        return clientMessageStatus;
    }

    public void setInput(ByteBuffer input) {
        this.input = input;
    }

    public void setOutput(ByteBuffer output) {
        this.output = output;
    }

    public void setClientMessageStatus(ClientMessageStatus clientMessageStatus) {
        this.clientMessageStatus = clientMessageStatus;
    }

    public SelectionKey getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(SelectionKey remoteServer) {
        this.remoteServer = remoteServer;
    }
}
