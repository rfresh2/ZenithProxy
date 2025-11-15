package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientConnectEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.module.ReplayStartedEvent;
import com.zenith.event.module.ReplayStoppedEvent;
import com.zenith.event.player.PlayerConnectedEvent;
import com.zenith.event.player.PlayerDisconnectedEvent;
import com.zenith.feature.replay.ReplayModPacketHandlerCodec;
import com.zenith.feature.replay.ReplayRecording;
import com.zenith.module.api.Module;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.util.config.Config.Client.Extra.ReplayMod.AutoRecordMode;
import lombok.Locked;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class ReplayMod extends Module {
    private final Path replayDirectory = Paths.get("replays");
    private ReplayRecording replayRecording = new ReplayRecording(replayDirectory);
    private final ReplayModPersistentEventListener persistentEventListener = new ReplayModPersistentEventListener(this);
    private @Nullable ScheduledFuture<?> delayedRecordingStopFuture;

    public ReplayMod() {
        super();
        persistentEventListener.subscribeEvents();
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientDisconnectEvent.class, this::onDisconnectEvent),
            of(ClientTickEvent.class, this::onClientTick),
            of(PlayerDisconnectedEvent.class, this::handleProxyClientDisconnectedEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return false;
    }

    @Override
    public PacketHandlerCodec registerClientPacketHandlerCodec() {
        return new ReplayModPacketHandlerCodec(this, Integer.MIN_VALUE, "replay-mod");
    }

    @Override
    public void onEnable() {
        startRecording();
    }

    @Override
    public void onDisable() {
        stopRecording();
    }

    @Locked
    public void startDelayedRecordingStop(int delaySeconds, BooleanSupplier condition) {
        cancelDelayedRecordingStop();
        scheduleRecordingStop(delaySeconds, condition);
    }

    @Locked
    private void cancelDelayedRecordingStop() {
        if (delayedRecordingStopFuture != null && !delayedRecordingStopFuture.isDone()) {
            debug("Cancelling delayed recording stop");
            delayedRecordingStopFuture.cancel(false);
            delayedRecordingStopFuture = null;
        }
    }

    @Locked
    private void scheduleRecordingStop(int delaySeconds, BooleanSupplier condition) {
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
        if (!replayRecording.ready()) return;
        var startT = replayRecording.getStartT();
        if (startT == 0L) return;
        if (CONFIG.client.extra.replayMod.maxRecordingTimeMins <= 0) return;
        if (System.currentTimeMillis() - ((long) CONFIG.client.extra.replayMod.maxRecordingTimeMins * 60 * 1000) > startT) {
            info("Stopping recording due to max recording time");
            disable();
        }
    }

    public void onInboundPacket(final Packet packet, final Session session) {
        if (!replayRecording.ready()) return;
        try {
            replayRecording.handleInboundPacket(System.nanoTime(), (MinecraftPacket) packet, session);
        } catch (final Throwable e) {
            error("Failed to handle inbound packet", e);
        }
    }

    public void onPostOutgoing(final Packet packet, final Session session) {
        if (!replayRecording.ready()) return;
        try {
            replayRecording.handleOutgoingPacket(System.nanoTime(), (MinecraftPacket) packet, session);
        } catch (final Throwable e) {
            error("Failed to handle outgoing packet", e);
        }
    }

    public void onDisconnectEvent(final ClientDisconnectEvent event) {
        disable();
    }

    /**
     * Consumers should call enable/disable instead of start/stop recording
     */
    @Locked
    private void startRecording() {
        cancelDelayedRecordingStop();
        info("Starting recording");
        this.replayRecording = new ReplayRecording(replayDirectory);
        try {
            this.replayRecording.startRecording();
            EVENT_BUS.postAsync(new ReplayStartedEvent());
            inGameAlert("<red>Recording started");
        } catch (final Exception e) {
            error("Failed to start recording", e);
            disable();
        }
    }

    @Locked
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
        inGameAlert("<red>Recording stopped");
        cancelDelayedRecordingStop();
    }

    public void handleProxyClientDisconnectedEvent(final PlayerDisconnectedEvent event) {
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
            info("Stopping recording due to player disconnect");
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
                of(PlayerConnectedEvent.class, this::handleProxyClientConnectedEvent),
                of(ClientConnectEvent.class, this::handleConnectEvent),
                of(PlayerHealthChangedEvent.class, this::handleHealthChangeEvent)
            );
        }

        public void handleProxyClientConnectedEvent(final PlayerConnectedEvent event) {
            if (instance.isEnabled()) return;
            if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED) {
                instance.info("Starting recording because player connected");
                instance.enable();
            }
        }

        public void handleConnectEvent(ClientConnectEvent event) {
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
