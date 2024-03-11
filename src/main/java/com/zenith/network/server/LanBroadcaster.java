package com.zenith.network.server;

import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.google.common.base.Suppliers;
import com.zenith.util.ComponentSerializer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private final Supplier<DatagramPacket> motdSupplier;
    private final Supplier<InetAddress> broadcastAddressSupplier = Suppliers.memoize(() -> {
        try {
            return InetAddress.getByAddress(new byte[]{(byte) 224, 0, 2, 60});
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    });

    public LanBroadcaster(ServerInfoBuilder serverInfoBuilder) {
        this.serverInfoBuilder = serverInfoBuilder;
        // micro-optimization to reduce cpu load
        this.motdSupplier = Suppliers.memoizeWithExpiration(() -> {
            try {
                var bytes = ("[MOTD]" + stripLegacyFormatting(ComponentSerializer.serializePlain(this.serverInfoBuilder.buildInfo(null).getDescription())) + "[/MOTD]"
                    + "[AD]" + CONFIG.server.bind.port + "[/AD]")
                    .getBytes();
                return new DatagramPacket(bytes, bytes.length, broadcastAddressSupplier.get(), 4445);
            } catch (final Exception e) {
                var bytes = ("[MOTD] ZenithProxy - " + CONFIG.authentication.username + "[/MOTD][AD]" + CONFIG.server.bind.port + "[/AD]").getBytes();
                return new DatagramPacket(bytes, bytes.length, broadcastAddressSupplier.get(), 4445);
            }
        }, 10, TimeUnit.SECONDS);
    }

    public void start() {
        errorCount.set(0);
        broadcastFuture = SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::broadcast, 0, 5, TimeUnit.SECONDS);
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
            if (!datagramSocket.getBroadcast())
                datagramSocket.setBroadcast(true);
            datagramSocket.send(motdSupplier.get());
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
