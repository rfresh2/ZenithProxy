package com.zenith.client;

import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.zenith.Proxy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.zenith.util.Constants.CLIENT_LOG;


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

    public String getDisconnectReason() {
        try {
            return this.disconnectFuture.get();
        } catch (ExecutionException e)  {
            return e.toString();
        } catch (Exception e) {
            PUnsafe.throwException(e);
            return null;
        }
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
    }

    @Override
    public void connect(boolean wait) {
        super.connect(wait);
    }
}
