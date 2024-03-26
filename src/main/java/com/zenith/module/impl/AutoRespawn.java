package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.zenith.Proxy;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.module.Module;

import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class AutoRespawn extends Module {
    private static final int tickEventRespawnDelay = 100;
    private int tickCounter = 0;
    public AutoRespawn() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            of(ClientBotTick.class, this::handleClientTickEvent),
                            of(DeathEvent.class, this::handleDeathEvent)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoRespawn.enabled;
    }


    public void handleDeathEvent(final DeathEvent event) {
        tickCounter = -tickEventRespawnDelay - (CONFIG.client.extra.autoRespawn.delayMillis / 50);
        EXECUTOR.schedule(this::checkAndRespawn, Math.max(CONFIG.client.extra.autoRespawn.delayMillis, 1000), TimeUnit.MILLISECONDS);
    }


    public void handleClientTickEvent(final ClientBotTick event) {
        // the purpose of this handler is to also autorespawn when we've logged in and are already dead
        if (tickCounter++ < tickEventRespawnDelay) return;
        tickCounter = 0;
        checkAndRespawn();
    }

    private void checkAndRespawn() {
        if (Proxy.getInstance().isConnected() && CACHE.getPlayerCache().getThePlayer().getHealth() <= 0 && isNull(
                Proxy.getInstance().getCurrentPlayer().get())) {
            MODULE_LOG.info("Performing AutoRespawn");
            sendClientPacketAsync(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
            CACHE.getPlayerCache().getThePlayer().setHealth(20.0f);
        }
    }
}
