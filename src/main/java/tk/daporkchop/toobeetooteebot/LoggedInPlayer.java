package tk.daporkchop.toobeetooteebot;

import org.java_websocket.WebSocket;

public class LoggedInPlayer {

    public long lastUsed;
    public long lastSentMessage;
    public RegisteredPlayer player;
    public WebSocket clientSocket;

    public LoggedInPlayer(RegisteredPlayer player, WebSocket clientSocket)    {
        this.player = player;
        this.lastUsed = System.currentTimeMillis();
        this.clientSocket = clientSocket;
    }
}
