package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.module.ReplayStartedEvent;
import com.zenith.event.module.ReplayStoppedEvent;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.feature.replay.ReplayModPacketHandlerCodec;
import com.zenith.feature.replay.ReplayRecording;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.util.ComponentSerializer;
import com.zenith.util.Config.Client.Extra.ReplayMod.AutoRecordMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class ReplayMod extends Module {
    private final PacketHandlerCodec codec = new ReplayModPacketHandlerCodec(this, Integer.MIN_VALUE, "replay-mod");
    private final Path replayDirectory = Paths.get("replays");
    private ReplayRecording replayRecording = new ReplayRecording(replayDirectory);
    private final ReplayModPersistentEventListener persistentEventListener = new ReplayModPersistentEventListener();

    public ReplayMod() {
        super();
        persistentEventListener.subscribeEvents();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(DisconnectEvent.class, this::onDisconnectEvent),
            of(ClientTickEvent.class, this::onClientTick),
            of(ProxyClientDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent)
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

    public void onClientTick(final ClientTickEvent event) {
        var startT = replayRecording.getStartT();
        if (startT == 0L) return;
        if (CONFIG.client.extra.replayMod.maxRecordingTimeMins <= 0) return;
        if (Instant.now().toEpochMilli() - (CONFIG.client.extra.replayMod.maxRecordingTimeMins * 60 * 1000) > startT) {
            MODULE_LOG.info("Stopping ReplayMod recording due to max recording time");
            disable();
        }
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
            EVENT_BUS.postAsync(new ReplayStartedEvent());
            Proxy.getInstance().getActiveConnections().forEach(session -> {
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cReplay recording started"), false));
            });
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
        var file = replayRecording.getReplayFile();
        if (file.exists()) {
            MODULE_LOG.info("ReplayMod recording saved to {}", file.getPath());
            EVENT_BUS.postAsync(new ReplayStoppedEvent(replayRecording.getReplayFile()));
        } else {
            EVENT_BUS.postAsync(new ReplayStoppedEvent(null));
        }
        Proxy.getInstance().getActiveConnections().forEach(session -> {
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cReplay recording stopped"), false));
        });
    }

    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
            MODULE_LOG.info("Stopping ReplayMod recording due to player disconnect");
            disable();
        }
    }

    /**
     * Event listeners even when the module is disabled
     */
    public static class ReplayModPersistentEventListener {

        public void subscribeEvents() {
            EVENT_BUS.subscribe(
                this,
                of(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
                of(ConnectEvent.class, this::handleConnectEvent));
        }

        public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
                MODULE_LOG.info("Starting ReplayMod recording because player connected");
                MODULE.get(ReplayMod.class).enable();
            }
        }

        public void handleConnectEvent(ConnectEvent event) {
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PROXY_CONNECTED) {
                MODULE.get(ReplayMod.class).enable();
            }
        }
    }
}
