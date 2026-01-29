package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.module.api.Module;
import com.zenith.util.ChatUtil;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;

public class Spammer extends Module {
    private final Timer tickTimer = Timers.tickTimer();
    private int spamIndex = 0;
    private final HashSet<String> whisperedPlayers = new HashSet<>();

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientTickEvent.class, this::handleClientTickEvent),
            of(ClientTickEvent.Starting.class, this::clientTickStarting)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.spammer.enabled;
    }

    @Override
    public void onEnable() {
        whisperedPlayers.clear();
    }

    @Override
    public void onDisable() {
        whisperedPlayers.clear();
    }

    private void handleClientTickEvent(final ClientTickEvent event) {
        if (Proxy.getInstance().isInQueue()) return;
        if (!Proxy.getInstance().isOnlineForAtLeastDuration(Duration.ofSeconds(5))) return;
        if (!CONFIG.client.extra.spammer.whilePlayerConnected && Proxy.getInstance().hasActivePlayer()) return;
        if (tickTimer.tick(CONFIG.client.extra.spammer.delayTicks)) {
            sendSpam();
        }
    }

    private void clientTickStarting(final ClientTickEvent.Starting event) {
        tickTimer.reset();
        spamIndex = 0;
    }

    private void sendSpam() {
        if (CONFIG.client.extra.spammer.messages.isEmpty()) return;
        if (CONFIG.client.extra.spammer.randomOrder) {
            spamIndex = ThreadLocalRandom.current().nextInt(0, CONFIG.client.extra.spammer.messages.size());
        } else {
            spamIndex = (spamIndex + 1) % CONFIG.client.extra.spammer.messages.size();
        }
        var msg = CONFIG.client.extra.spammer.messages.get(spamIndex)
            + (CONFIG.client.extra.spammer.appendRandom ? " " + UUID.randomUUID().toString().substring(0, 6) : "");
        if (CONFIG.client.extra.spammer.whisper) {
            var player = getNextPlayer();
            if (player != null) {
                var packet = ChatUtil.getWhisperChatPacket(player, msg);
                debug("> {}", packet.getMessage());
                sendClientPacketAsync(packet);
            }
        } else {
            var packet = new ServerboundChatPacket(ChatUtil.sanitizeChatMessage(msg));
            debug("> {}", packet.getMessage());
            sendClientPacketAsync(packet);
        }
    }

    private @Nullable String getNextPlayer() {
        var nextPlayer = CACHE.getTabListCache().getEntries().stream()
            .map(PlayerListEntry::getName)
            .filter(name -> !name.equals(CONFIG.authentication.username))
            .filter(name -> !this.whisperedPlayers.contains(name))
            .findFirst();
        if (nextPlayer.isPresent()) {
            this.whisperedPlayers.add(nextPlayer.get());
            return nextPlayer.get();
        } else {
            if (this.whisperedPlayers.isEmpty()) return null;
            this.whisperedPlayers.clear();
            return getNextPlayer();
        }
    }
}
