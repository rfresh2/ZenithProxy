package com.zenith.module.impl;

import com.zenith.event.module.ClientBotTick;
import com.zenith.module.Module;
import com.zenith.util.Timer;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Set<String> playerNames = CACHE.getTabListCache().getEntries().stream()
                .map(PlayerListEntry::getName)
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

    public void clientTickStarting(final ClientBotTick.Starting event) {
        tickTimer.reset();
        spamIndex = 0;
    }
}
