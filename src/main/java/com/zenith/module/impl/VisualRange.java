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
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;

import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class VisualRange extends Module {

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
                    if (noPlayerInVisualRange()) {
                        MODULE.get(ReplayMod.class).startDelayedRecordingStop(
                            CONFIG.client.extra.visualRange.replayRecordingCooldownMins,
                            this::noPlayerInVisualRange
                        );
                    }
                }
                case ENEMY -> {
                    if (!isFriend && noEnemyInVisualRange()) {
                        MODULE.get(ReplayMod.class).startDelayedRecordingStop(
                            CONFIG.client.extra.visualRange.replayRecordingCooldownMins,
                            this::noEnemyInVisualRange
                        );
                    }
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

    private void startReplayRecording() {
        if (!MODULE.get(ReplayMod.class).isEnabled()) {
            info("Starting replay recording");
            MODULE.get(ReplayMod.class).enable();
        }
    }

    private boolean noEnemyInVisualRange() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityPlayer)
            .filter(entity -> !entity.equals(CACHE.getPlayerCache().getThePlayer()))
            .allMatch(entityPlayer -> PLAYER_LISTS.getFriendsList().contains(entityPlayer.getUuid()));
    }

    private boolean noPlayerInVisualRange() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof EntityPlayer)
            .allMatch(entity -> entity.equals(CACHE.getPlayerCache().getThePlayer()));
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
