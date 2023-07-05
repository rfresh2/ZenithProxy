package com.zenith.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import com.zenith.util.TickTimer;

import static com.zenith.util.Constants.CONFIG;

public class Spammer extends Module {
    private final TickTimer tickTimer = new TickTimer();
    private int spamIndex = 0;

    @Subscribe
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
        Proxy.getInstance().getClient().send(new ClientChatPacket(CONFIG.client.extra.spammer.messages.get(spamIndex)));
    }

    @Override
    public void clientTickStarting() {
        tickTimer.reset();
        spamIndex = 0;
    }
}
