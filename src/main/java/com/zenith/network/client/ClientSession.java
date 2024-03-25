package com.zenith.network.client;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import com.zenith.event.module.ClientOnlineTickEvent;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;


@Getter
@Setter
public class ClientSession extends TcpClientSession {
    private static DefaultEventLoopGroup CLIENT_EVENT_LOOP_GROUP = new DefaultEventLoopGroup(1, new DefaultThreadFactory("Client Event Loop"));
    protected boolean serverProbablyOff;
    protected long ping = 0L;

    private boolean inQueue = false;
    private int lastQueuePosition = Integer.MAX_VALUE;
    // in game
    private boolean online = false;
    private boolean disconnected = true;
    private ScheduledFuture clientConstantTickFuture = null;
    private final EventLoop clientEventLoop;

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol, ProxyInfo proxyInfo) {
        super(host, port, bindAddress, 0, protocol, proxyInfo);
        this.addListener(new ClientListener(this));
        this.clientEventLoop = CLIENT_EVENT_LOOP_GROUP.next();
    }

    public ClientSession(String host, int port, String bindAddress, MinecraftProtocol protocol) {
        this(host, port, bindAddress, protocol, null);
    }

    public void setOnline(final boolean online) {
        this.online = online;
        if (online) startClientTicks();
        else stopClientTicks();
    }

    public void setDisconnected(final boolean disconnected) {
        this.disconnected = disconnected;
        setOnline(false);
    }

    public synchronized void startClientTicks() {
        if (this.clientConstantTickFuture == null || this.clientConstantTickFuture.isDone()) {
            EVENT_BUS.post(ClientOnlineTickEvent.Starting.INSTANCE);
            this.clientConstantTickFuture = EXECUTOR.scheduleAtFixedRate(
                () -> EVENT_BUS.post(ClientOnlineTickEvent.INSTANCE), 0L, 10L, TimeUnit.SECONDS);
        }
    }

    public synchronized void stopClientTicks() {
        if (this.clientConstantTickFuture != null && !this.clientConstantTickFuture.isDone()) {
            this.clientConstantTickFuture.cancel(false);
            try {
                this.clientConstantTickFuture.get(1L, TimeUnit.SECONDS);
            } catch (final Exception e) {
                // fall through
            }
            EVENT_BUS.post(ClientOnlineTickEvent.Stopped.INSTANCE);
            this.clientConstantTickFuture = null;
        }
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
