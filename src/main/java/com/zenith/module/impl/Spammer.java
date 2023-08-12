package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;

import java.util.UUID;
import java.util.function.Consumer;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.util.Pair.of;

public class Spammer extends Module {
    private final TickTimer tickTimer = new TickTimer();
    private final Subscription eventSubscription;
    private int spamIndex = 0;


    public Spammer() {
        this.eventSubscription = EVENT_BUS.subscribe(
                of(ClientTickEvent.class, (Consumer<ClientTickEvent>)this::handleClientTickEvent));
    }
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.spammer.enabled) {
            if (tickTimer.tick(CONFIG.client.extra.spammer.delayTicks, true)) {
                sendSpam();
            }
        }
    }

    private void sendSpam() {
        if (CONFIG.client.extra.spammer.messages.isEmpty()) return;
        if (CONFIG.client.extra.spammer.randomOrder) {
            spamIndex = (int) (Math.random() * CONFIG.client.extra.spammer.messages.size());
        } else {
            spamIndex = (spamIndex + 1) % CONFIG.client.extra.spammer.messages.size();
        }
        sendClientPacketAsync(new ClientChatPacket(CONFIG.client.extra.spammer.messages.get(spamIndex) + (CONFIG.client.extra.spammer.appendRandom ? " " + UUID.randomUUID().toString().substring(0, 6) : "")));
    }

    @Override
    public void clientTickStarting() {
        tickTimer.reset();
        spamIndex = 0;
    }
}
