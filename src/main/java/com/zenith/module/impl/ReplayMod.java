package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.feature.replay.ReplayModPacketHandlerCodec;
import com.zenith.feature.replay.ReplayRecording;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.ZenithHandlerCodec;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.Shared.MODULE_LOG;

public class ReplayMod extends Module {
    private final PacketHandlerCodec codec = new ReplayModPacketHandlerCodec(this, Integer.MIN_VALUE, "replay-mod");
    private final Path replayDirectory = Paths.get("replays");
    private ReplayRecording replayRecording = new ReplayRecording(replayDirectory);

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(DisconnectEvent.class, this::onDisconnectEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return false;
    }

    @Override
    public void onEnable() {
        startRecording();
        ZenithHandlerCodec.CLIENT_REGISTRY.register(codec);
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.CLIENT_REGISTRY.unregister(codec);
        stopRecording();
    }

    public void onInboundPacket(final Packet packet, final Session session) {
        replayRecording.handleInboundPacket(Instant.now().toEpochMilli(), (MinecraftPacket) packet, session);
    }

    public void onPostOutgoing(final Packet packet, final Session session) {
        replayRecording.handleOutgoingPacket(Instant.now().toEpochMilli(), (MinecraftPacket) packet, session);
    }

    public void onDisconnectEvent(final DisconnectEvent event) {
        disable();
    }

    /**
     * Consumers should call enable/disable instead of start/stop recording
     */
    private void startRecording() {
        MODULE_LOG.info("Starting ReplayMod recording");
        this.replayRecording = new ReplayRecording(replayDirectory);
        try {
            this.replayRecording.startRecording();
        } catch (final Exception e) {
            MODULE_LOG.error("Failed to start ReplayMod recording", e);
            disable();
        }
    }

    private void stopRecording() {
        MODULE_LOG.info("Stopping ReplayMod recording");
        try {
            this.replayRecording.close();
        } catch (final Exception e) {
            MODULE_LOG.error("Failed to save ReplayMod recording", e);
        }
    }
}
