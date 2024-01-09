package com.zenith.network.client;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.io.IOException;

import static com.zenith.Shared.CLIENT_LOG;


@Getter
@Setter
public class ClientSession extends TcpClientSession {
    protected boolean serverProbablyOff;
    protected long ping = 0L;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean disconnected = true;

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol) {
        super(host, port, bindAddress, 0, protocol);
        this.addListener(new ClientListener(this));
    }

    @Override
    public void disconnect(Component reason, Throwable cause) {
        super.disconnect(reason, cause);
        serverProbablyOff = false;
        if (cause == null) {
            serverProbablyOff = true;
        } else if (cause instanceof IOException)    {
            CLIENT_LOG.error("Error during client disconnect", cause);
        } else {
            CLIENT_LOG.error("", cause);
        }
        this.online = false;
    }

    @Override
    public void connect(boolean wait) {
        super.connect(wait);
    }
}
