package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.event.module.ClientBotTick;
import com.zenith.module.Module;
import com.zenith.util.Timer;

import java.util.HashSet;
import java.util.UUID;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class Spammer extends Module {
    private final Timer tickTimer = Timer.newTickTimer();
    private int spamIndex = 0;
    private final HashSet<String> whisperedPlayers = new HashSet<>();


    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientTickEvent),
            of(ClientBotTick.Starting.class, this::clientTickStarting)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.spammer.enabled;
    }

    public void handleClientTickEvent(final ClientBotTick event) {
        if (tickTimer.tick(CONFIG.client.extra.spammer.delayTicks)) {
            sendSpam();
        }
    }

    private void sendSpam() {
        if (CONFIG.client.extra.spammer.messages.isEmpty()) return;
        if (CONFIG.client.extra.spammer.randomOrder) {
            spamIndex = (int) (Math.random() * CONFIG.client.extra.spammer.messages.size());
        } else {
            spamIndex = (spamIndex + 1) % CONFIG.client.extra.spammer.messages.size();
        }
        if (CONFIG.client.extra.spammer.whisper) {
            String player = getNextPlayer();
            if (player != null) {
                sendClientPacketAsync(new ServerboundChatPacket("/w " + player + " " + CONFIG.client.extra.spammer.messages.get(spamIndex) + (CONFIG.client.extra.spammer.appendRandom ? " " + UUID.randomUUID().toString().substring(0, 6) : "")));
            }
        } else {
            sendClientPacketAsync(new ServerboundChatPacket(CONFIG.client.extra.spammer.messages.get(spamIndex) + (CONFIG.client.extra.spammer.appendRandom ? " " + UUID.randomUUID().toString().substring(0, 6) : "")));
        }

    }

    private String getNextPlayer() {
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

    public void clientTickStarting(final ClientBotTick.Starting event) {
        tickTimer.reset();
        spamIndex = 0;
    }
}
