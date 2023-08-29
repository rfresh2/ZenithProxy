package com.zenith.module.impl;

import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;

import java.util.function.Supplier;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;

public class Spammer extends Module {
    private final TickTimer tickTimer = new TickTimer();
    private int spamIndex = 0;


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
        // todo: new chat packet
//        sendClientPacketAsync(new ClientChatPacket(CONFIG.client.extra.spammer.messages.get(spamIndex) + (CONFIG.client.extra.spammer.appendRandom ? " " + UUID.randomUUID().toString().substring(0, 6) : "")));
    }

    @Override
    public void clientTickStarting() {
        tickTimer.reset();
        spamIndex = 0;
    }
}
