package com.zenith.network.client;

import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.zenith.Proxy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.zenith.Shared.CLIENT_LOG;


@Getter
@Setter
public class ClientSession extends TcpClientSession {
    @Getter(AccessLevel.PRIVATE)
    protected final CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
    protected final Proxy proxy;
    protected boolean serverProbablyOff;
    protected long ping = 0L;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean disconnected = true;

    public ClientSession(String host, int port, PacketProtocol protocol, @NonNull Proxy proxy) {
        super(host, port, protocol);
        this.proxy = proxy;
        this.addListener(new ClientListener(this.proxy, this));
    }

    @Override
    public void disconnect(String reason, Throwable cause) {
        super.disconnect(reason, cause);
        serverProbablyOff = false;
        if (cause == null) {
            serverProbablyOff = true;
            this.disconnectFuture.complete(reason);
        } else if (cause instanceof IOException)    {
            this.disconnectFuture.complete(String.format("IOException: %s", cause.getMessage()));
        } else {
            CLIENT_LOG.error("", cause);
            this.disconnectFuture.completeExceptionally(cause);
        }
        this.online = false;
    }

    @Override
    public void connect(boolean wait) {
        super.connect(wait);
    }
}
