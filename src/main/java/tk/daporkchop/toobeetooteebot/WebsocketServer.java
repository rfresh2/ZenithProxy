package tk.daporkchop.toobeetooteebot;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

public class WebsocketServer extends WebSocketServer {
    public WebsocketServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public WebsocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("connect ");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (TooBeeTooTeeBot.INSTANCE.tabHeader != null && TooBeeTooTeeBot.INSTANCE.tabFooter != null) {
                    String header = TooBeeTooTeeBot.INSTANCE.tabHeader.getFullText();
                    String footer = TooBeeTooTeeBot.INSTANCE.tabFooter.getFullText();
                    conn.send("tabDiff " + header + " " + footer);
                } else if (TooBeeTooTeeBot.INSTANCE.tabHeader != null)  {
                    String header = TooBeeTooTeeBot.INSTANCE.tabHeader.getFullText();
                    conn.send("tabDiff " + header + "  ");
                } else if (TooBeeTooTeeBot.INSTANCE.tabFooter != null)  {
                    String footer = TooBeeTooTeeBot.INSTANCE.tabFooter.getFullText();
                    conn.send("tabDiff   " + footer);
                }
                for (TabListPlayer entry : TooBeeTooTeeBot.INSTANCE.playerListEntries)    {
                    conn.send("tabAdd  " + entry.name + " " + entry.ping);
                }
                conn.send("tabAdd  2pork2bot " + 1);
            }
        }, 1000);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //System.out.println(conn.getResourceDescriptor() + " disconnected with code " + code + " and reason " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //TODO: handle
    }

    @Override
    public void onFragment(WebSocket conn, Framedata fragment) {
        System.out.println("received fragment: " + fragment);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            conn.send("error   ");
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket started!");
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        //System.out.println(text);
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }
}
