package tk.daporkchop.toobeetooteebot;

import org.java_websocket.WebSocket;

/**
 * Created by DaPorkchop_ on 5/12/2017.
 */
public class LoggedInPlayer {

    public long lastUsed;
    public RegisteredPlayer player;
    public WebSocket clientSocket;

    public LoggedInPlayer(RegisteredPlayer player, WebSocket clientSocket)    {
        this.player = player;
        this.lastUsed = System.currentTimeMillis();
        this.clientSocket = clientSocket;
    }
}
