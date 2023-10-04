package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;

public class Spammer extends Module {
    private final TickTimer tickTimer = new TickTimer();
    private int spamIndex = 0;
    private final HashSet<String> whisperedPlayers = new HashSet<>();


    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(ClientTickEvent.class, this::handleClientTickEvent);
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.spammer.enabled;
    }

    public void handleClientTickEvent(final ClientTickEvent event) {
        if (tickTimer.tick(CONFIG.client.extra.spammer.delayTicks, true)) {
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
        Set<String> playerNames = CACHE.getTabListCache().getTabList().getEntries().stream()
                .map(PlayerEntry::getName)
                .collect(Collectors.toSet());
        if (playerNames.size() == 1) { return null; } // no other players connected
        playerNames.removeAll(this.whisperedPlayers);
        playerNames.remove(CONFIG.authentication.username);
        if (!playerNames.isEmpty()) { // online players who haven't been messaged yet
            String nextPlayer = playerNames.stream().toList().getFirst();
            this.whisperedPlayers.add(nextPlayer);
            return nextPlayer;
        } else { // every player has been messaged, restarting cycle
            this.whisperedPlayers.clear();
            return getNextPlayer();
        }

    }

    @Override
    public void clientTickStarting() {
        tickTimer.reset();
        spamIndex = 0;
    }
}
