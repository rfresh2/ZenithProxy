package com.zenith.module.impl;

import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.module.PlayerHealthChangedEvent;
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
import com.zenith.util.Config.Client.Extra.ReplayMod.AutoRecordMode;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class ReplayMod extends Module {
    private final PacketHandlerCodec codec = new ReplayModPacketHandlerCodec(this, Integer.MIN_VALUE, "replay-mod");
    private final Path replayDirectory = Paths.get("replays");
    private ReplayRecording replayRecording = new ReplayRecording(replayDirectory);
    private final ReplayModPersistentEventListener persistentEventListener = new ReplayModPersistentEventListener(this);
    private @Nullable ScheduledFuture<?> delayedRecordingStopFuture;

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

    public synchronized void startDelayedRecordingStop(int delaySeconds, BooleanSupplier condition) {
        cancelDelayedRecordingStop();
        scheduleRecordingStop(delaySeconds, condition);
    }

    private synchronized void cancelDelayedRecordingStop() {
        if (delayedRecordingStopFuture != null && !delayedRecordingStopFuture.isDone()) {
            debug("Cancelling delayed recording stop");
            delayedRecordingStopFuture.cancel(false);
            delayedRecordingStopFuture = null;
        }
    }

    private synchronized void scheduleRecordingStop(int delaySeconds, BooleanSupplier condition) {
        delayedRecordingStopFuture = EXECUTOR.schedule(() -> disableReplayRecordingConditional(condition), delaySeconds, TimeUnit.SECONDS);
    }

    private void disableReplayRecordingConditional(BooleanSupplier condition) {
        if (!isEnabled()) return;
        if (condition.getAsBoolean()) {
            info("Delayed recording stop condition met");
            disable();
        } else {
            scheduleRecordingStop(30, condition);
        }
    }

    public void onClientTick(final ClientTickEvent event) {
        var startT = replayRecording.getStartT();
        if (startT == 0L) return;
        if (CONFIG.client.extra.replayMod.maxRecordingTimeMins <= 0) return;
        if (System.currentTimeMillis() - ((long) CONFIG.client.extra.replayMod.maxRecordingTimeMins * 60 * 1000) > startT) {
            info("Stopping recording due to max recording time");
            disable();
        }
    }

    public void onInboundPacket(final Packet packet, final Session session) {
        try {
            replayRecording.handleInboundPacket(System.currentTimeMillis(), (MinecraftPacket) packet, session);
        } catch (final Throwable e) {
            error("Failed to handle inbound packet", e);
        }
    }

    public void onPostOutgoing(final Packet packet, final Session session) {
        try {
            replayRecording.handleOutgoingPacket(System.currentTimeMillis(), (MinecraftPacket) packet, session);
        } catch (final Throwable e) {
            error("Failed to handle outgoing packet", e);
        }
    }

    public void onDisconnectEvent(final DisconnectEvent event) {
        disable();
    }

    /**
     * Consumers should call enable/disable instead of start/stop recording
     */
    private void startRecording() {
        cancelDelayedRecordingStop();
        info("Starting recording");
        this.replayRecording = new ReplayRecording(replayDirectory);
        try {
            this.replayRecording.startRecording();
            EVENT_BUS.postAsync(new ReplayStartedEvent());
            inGameAlert("&cRecording started");
        } catch (final Exception e) {
            error("Failed to start recording", e);
            disable();
        }
    }

    private void stopRecording() {
        info("Stopping recording");
        try {
            this.replayRecording.close();
        } catch (final Exception e) {
            error("Failed to save recording", e);
        }
        var file = replayRecording.getReplayFile();
        if (file.exists()) {
            info("Recording saved to {}", file.getPath());
            EVENT_BUS.postAsync(new ReplayStoppedEvent(replayRecording.getReplayFile()));
        } else {
            EVENT_BUS.postAsync(new ReplayStoppedEvent(null));
        }
        inGameAlert("&cRecording stopped");
        cancelDelayedRecordingStop();
    }

    public void handleProxyClientDisconnectedEvent(final ProxyClientDisconnectedEvent event) {
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
            info("Stopping recording due to player disconnect");
            disable();
        }
    }

    public void handleHealthChangeEvent(PlayerHealthChangedEvent event) {
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.HEALTH
            && event.newHealth() > CONFIG.client.extra.replayMod.replayRecordingHealthThreshold) {
            info("Stopping recording due to health above: {}", CONFIG.client.extra.replayMod.replayRecordingHealthThreshold);
            disable();
        }
    }

    /**
     * Event listeners even when the module is disabled
     */
    public static class ReplayModPersistentEventListener {
        private final ReplayMod instance;

        public ReplayModPersistentEventListener(ReplayMod instance) {
            this.instance = instance;
        }

        public void subscribeEvents() {
            EVENT_BUS.subscribe(
                this,
                of(ProxyClientConnectedEvent.class, this::handleProxyClientConnectedEvent),
                of(ConnectEvent.class, this::handleConnectEvent),
                of(PlayerHealthChangedEvent.class, this::handleHealthChangeEvent)
            );
        }

        public void handleProxyClientConnectedEvent(final ProxyClientConnectedEvent event) {
            if (instance.isEnabled()) return;
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
                instance.info("Starting recording because player connected");
                instance.enable();
            }
        }

        public void handleConnectEvent(ConnectEvent event) {
            if (instance.isEnabled()) return;
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PROXY_CONNECTED) {
                instance.info("Starting recording because proxy connected");
                instance.enable();
            }
        }

        public void handleHealthChangeEvent(PlayerHealthChangedEvent event) {
            if (instance.isEnabled()) return;
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.HEALTH
                && event.newHealth() <= CONFIG.client.extra.replayMod.replayRecordingHealthThreshold) {
                instance.info("Starting recording because health is below {}", CONFIG.client.extra.replayMod.replayRecordingHealthThreshold);
                instance.enable();
                instance.startDelayedRecordingStop(
                    30,
                    () -> CACHE.getPlayerCache().getThePlayer().getHealth() > CONFIG.client.extra.replayMod.replayRecordingHealthThreshold
                );
            }
        }
    }
}
