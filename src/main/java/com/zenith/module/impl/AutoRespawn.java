package com.zenith.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.DeathEvent;
import com.zenith.module.Module;
import com.zenith.util.Wait;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super();
    }

    @Subscribe
    public void handleDeathEvent(final DeathEvent event) {
        if (CONFIG.client.extra.autoRespawn.enabled) {
            Wait.waitALittleMs(Math.max(CONFIG.client.extra.autoRespawn.delayMillis, 1000));
            if (Proxy.getInstance().isConnected() && CACHE.getPlayerCache().getThePlayer().getHealth() <= 0 && isNull(Proxy.getInstance().getCurrentPlayer().get())) {
                MODULE_LOG.info("Performing AutoRespawn");
                Proxy.getInstance().getClient().send(new ClientRequestPacket(ClientRequest.RESPAWN));

                // todo: remove this and only stop trying to respawn once we actually respawn
                //  i.e. we received a respawn packet and our health > 0
                //  also check we're actually in survival mode, not sure if spectator or creative does weird stuff
                CACHE.getPlayerCache().getThePlayer().setHealth(20.0f);
            }
        }
    }
}
