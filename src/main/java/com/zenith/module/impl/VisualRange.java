package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.VisualRangeEnterEvent;
import com.zenith.event.module.VisualRangeLeaveEvent;
import com.zenith.event.module.VisualRangeLogoutEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.event.proxy.PlayerLeftVisualRangeEvent;
import com.zenith.event.proxy.PlayerLogoutInVisualRangeEvent;
import com.zenith.module.Module;
import com.zenith.util.Config.Client.Extra.ReplayMod.AutoRecordMode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class VisualRange extends Module {

    private @Nullable ScheduledFuture<?> visualRangeLeaveRecordingStopFuture;
    private Instant lastWhisper = Instant.EPOCH;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(NewPlayerInVisualRangeEvent.class, this::handleNewPlayerInVisualRangeEvent),
            of(PlayerLeftVisualRangeEvent.class, this::handlePlayerLeftVisualRangeEvent),
            of(PlayerLogoutInVisualRangeEvent.class, this::handlePlayerLogoutInVisualRangeEvent),
            of(VisualRangeEnterEvent.class, this::enterWhisperHandler)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.visualRange.enabled;
    }

    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        var isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntity().getUuid());
        if (CONFIG.client.extra.visualRange.replayRecording) {
            switch (CONFIG.client.extra.visualRange.replayRecordingMode) {
                case ALL -> startReplayRecording();
                case ENEMY -> {
                    if (!isFriend) startReplayRecording();
                }
            }
        }
        if (isFriend && CONFIG.client.extra.visualRange.ignoreFriends) {
            debug("Ignoring enter alert for friend: {}", event.playerEntry().getName());
            return;
        }
        if (CONFIG.client.extra.visualRange.enterAlert) {
            warn("{} entered visual range [{}, {}, {}]", event.playerEntry().getName(), event.playerEntity().getX(), event.playerEntity().getY(), event.playerEntity().getZ());
            EVENT_BUS.post(new VisualRangeEnterEvent(event.playerEntry(), event.playerEntity(), isFriend));
        }
    }

    public void enterWhisperHandler(VisualRangeEnterEvent event) {
        if (!CONFIG.client.extra.visualRange.enterWhisper) return;
        if (CONFIG.client.extra.visualRange.enterWhisperWhilePlayerConnected && Proxy.getInstance().hasActivePlayer()) return;
        if (Instant.now().minusSeconds(CONFIG.client.extra.visualRange.enterWhisperCooldownSeconds).isBefore(lastWhisper)) return;
        lastWhisper = Instant.now();
        sendClientPacketAsync(new ServerboundChatCommandPacket("w " + event.playerEntry().getName() + " " + CONFIG.client.extra.visualRange.enterWhisperMessage));
    }

    public void handlePlayerLeftVisualRangeEvent(final PlayerLeftVisualRangeEvent event) {
        var isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntity().getUuid());
        if (CONFIG.client.extra.visualRange.replayRecording) {
            switch (CONFIG.client.extra.visualRange.replayRecordingMode) {
                case ALL -> {
                    if (!anyPlayerInVisualRange()) scheduleRecordingStop();
                }
                case ENEMY -> {
                    if (!isFriend && !anyEnemyInVisualRange()) scheduleRecordingStop();
                }
            }
        }

        if (isFriend && CONFIG.client.extra.visualRange.ignoreFriends) {
            debug("Ignoring leave alert for friend: {}", event.playerEntry().getName());
            return;
        }
        if (CONFIG.client.extra.visualRange.leaveAlert) {
            warn("{} left visual range [{}, {}, {}]", event.playerEntry().getName(), event.playerEntity().getX(), event.playerEntity().getY(), event.playerEntity().getZ());
            EVENT_BUS.post(new VisualRangeLeaveEvent(event.playerEntry(), event.playerEntity(), isFriend));
        }
    }

    private void scheduleRecordingStop() {
        cancelVisualRangeLeaveRecordingStopFuture();
        visualRangeLeaveRecordingStopFuture = EXECUTOR.schedule(
            this::disableReplayRecordingConditional,
            CONFIG.client.extra.visualRange.replayRecordingCooldownMins,
            TimeUnit.MINUTES
        );
    }

    private void startReplayRecording() {
        if (!MODULE.get(ReplayMod.class).isEnabled()) {
            info("Starting replay recording");
            MODULE.get(ReplayMod.class).enable();
        }
        cancelVisualRangeLeaveRecordingStopFuture();
    }

    private void cancelVisualRangeLeaveRecordingStopFuture() {
        if (visualRangeLeaveRecordingStopFuture != null && !visualRangeLeaveRecordingStopFuture.isDone()) {
            visualRangeLeaveRecordingStopFuture.cancel(false);
        }
    }

    private void disableReplayRecordingConditional() {
        if (!MODULE.get(ReplayMod.class).isEnabled()) return;
        if (anyEnemyInVisualRange()) return;
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PROXY_CONNECTED) return;
        if (CONFIG.client.extra.replayMod.autoRecordMode == AutoRecordMode.PLAYER_CONNECTED)
            if (Proxy.getInstance().hasActivePlayer()) return;
        info("Stopping replay recording");
        MODULE.get(ReplayMod.class).disable();
    }

    private boolean anyEnemyInVisualRange() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityPlayer)
            .filter(entity -> !entity.equals(CACHE.getPlayerCache().getThePlayer()))
            .anyMatch(entityPlayer -> !PLAYER_LISTS.getFriendsList().contains(entityPlayer.getUuid()));
    }

    private boolean anyPlayerInVisualRange() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityPlayer)
            .anyMatch(entity -> !entity.equals(CACHE.getPlayerCache().getThePlayer()));
    }

    public void handlePlayerLogoutInVisualRangeEvent(final PlayerLogoutInVisualRangeEvent event) {
        if (!CONFIG.client.extra.visualRange.logoutAlert) return;
        var isFriend = PLAYER_LISTS.getFriendsList().contains(event.playerEntity().getUuid());
        if (isFriend && CONFIG.client.extra.visualRange.ignoreFriends) {
            debug("Ignoring logout alert for friend: {}", event.playerEntry().getName());
            return;
        }
        warn("{} logged out in visual range [{}, {}, {}]", event.playerEntry().getName(), event.playerEntity().getX(), event.playerEntity().getY(), event.playerEntity().getZ());
        EVENT_BUS.post(new VisualRangeLogoutEvent(event.playerEntry(), event.playerEntity(), isFriend));
    }
}
