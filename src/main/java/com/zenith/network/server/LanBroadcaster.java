package com.zenith.network.server;

import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.google.common.base.Suppliers;
import com.zenith.util.ComponentSerializer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.zenith.Shared.*;

public class LanBroadcaster {

    private ScheduledFuture<?> broadcastFuture;
    private final ServerInfoBuilder serverInfoBuilder;
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private DatagramSocket datagramSocket;
    private final Supplier<byte[]> motdSupplier;

    public LanBroadcaster(ServerInfoBuilder serverInfoBuilder) {
        this.serverInfoBuilder = serverInfoBuilder;
        // micro-optimization to reduce cpu load
        this.motdSupplier = Suppliers.memoizeWithExpiration(() -> {
            try {
                return ("[MOTD]" + stripLegacyFormatting(ComponentSerializer.serializePlain(this.serverInfoBuilder.buildInfo(null).getDescription())) + "[/MOTD]"
                    + "[AD]" + CONFIG.server.bind.port + "[/AD]")
                    .getBytes();
            } catch (final Exception e) {
                return ("[MOTD] ZenithProxy - " + CONFIG.authentication.username + "[/MOTD][AD]" + CONFIG.server.bind.port + "[/AD]").getBytes();
            }
        }, 10, TimeUnit.SECONDS);
    }

    public void start() {
        errorCount.set(0);
        broadcastFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::broadcast, 0, 1500, TimeUnit.MILLISECONDS);
        SERVER_LOG.info("Started LAN server broadcaster");
    }

    public void stop() {
        if (broadcastFuture != null) {
            broadcastFuture.cancel(true);
            broadcastFuture = null;
        }
    }

    public void broadcast() {
        try {
            if (datagramSocket == null || datagramSocket.isClosed()) datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
            var bytes = motdSupplier.get();
            datagramSocket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getByName("224.0.2.60"), 4445));
            this.errorCount.set(0);
        } catch (final Exception e) {
            SERVER_LOG.error("Error broadcasting LAN server", e);
            var count = errorCount.incrementAndGet();
            if (count >= 5) {
                SERVER_LOG.error("Too many errors broadcasting LAN server, stopping broadcaster");
                stop();
            }
        }
    }

    private String stripLegacyFormatting(String string) {
        return string.replaceAll("(?i)§[0-9A-FK-OR]", "").replaceAll("\n", " | ");
    }
}
