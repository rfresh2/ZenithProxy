package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.feature.replay.ReplayModPacketHandlerCodec;
import com.zenith.feature.replay.ReplayRecording;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class ReplayMod extends Module {
    private PacketHandlerCodec codec;
    private final Path replayDirectory = Paths.get("replays");
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private ReplayRecording replayRecording;

    public ReplayMod() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        codec = new ReplayModPacketHandlerCodec(this, Integer.MIN_VALUE, "replay-mod");
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ConnectEvent.class, this::onConnectEvent),
            of(DisconnectEvent.class, this::onDisconnectEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.replayMod.enabled;
    }

    @Override
    public void onEnable() {
        ZenithHandlerCodec.CLIENT_REGISTRY.register(codec);
        // start recording?
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.CLIENT_REGISTRY.unregister(codec);
        if (recording.get()) {
            stopRecording();
        }
    }

    public void onInboundPacket(final Packet packet, final Session session) {
        if (recording.get()) {
            replayRecording.handleInboundPacket(Instant.now().toEpochMilli(), (MinecraftPacket) packet, session);
        }
    }

    public void onPostOutgoing(final Packet packet, final Session session) {
        if (recording.get()) {
            replayRecording.translateOutgoingPacket(Instant.now().toEpochMilli(), (MinecraftPacket) packet, session);
        }
    }

    public void onConnectEvent(final ConnectEvent event) {
        if (!recording.get()) {
            startRecording();
        }
    }

    public void onDisconnectEvent(final DisconnectEvent event) {
        if (recording.get()) {
            stopRecording();
        }
    }

    public void startRecording() {
        if (!recording.compareAndSet(false, true)) return;
        MODULE_LOG.info("Starting ReplayMod recording");
        this.replayRecording = new ReplayRecording(replayDirectory);
        try {
            this.replayRecording.startRecording();
        } catch (final Exception e) {
            MODULE_LOG.error("Failed to start ReplayMod recording", e);
            recording.set(false);
        }
    }

    @SneakyThrows
    public void stopRecording() {
        if (!recording.compareAndSet(true, false)) return;
        MODULE_LOG.info("Stopping ReplayMod recording");
        this.replayRecording.close();
    }
}
