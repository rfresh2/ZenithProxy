package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorUtils;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import lombok.NonNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class RespawnHandler implements AsyncPacketHandler<ClientboundRespawnPacket, ClientSession> {

    private final AtomicBoolean isSpectatorRespawning = new AtomicBoolean(false);

    @Override
    public boolean applyAsync(@NonNull ClientboundRespawnPacket packet, @NonNull ClientSession session) {
        // must send respawn packet before cache gets reset
        // lots of race conditions with packet sequence could happen
        if (isSpectatorRespawning.compareAndSet(false, true)) {
            /**
             * see https://c4k3.github.io/wiki.vg/Protocol.html#Respawn
             * If you must respawn a player in the same dimension without killing them,
             * send two respawn packets, one to a different world and then another to the
             * world you want. You do not need to complete the first respawn;
             * it only matters that you send two packets.
             */
            // we need this method to be invoked *after* the 2nd respawn packet
            // and we only want to invoke it once (on the first)
            // delay is a hacky workaround and might still get caught in race condition sometimes
            SCHEDULED_EXECUTOR_SERVICE.schedule(this::spectatorRespawn, 3L, TimeUnit.SECONDS);
        }
        if (!Objects.equals(CACHE.getChunkCache().getCurrentDimension().getDimensionName(), packet.getDimension())) {
            CACHE.reset(false);
            // only partial reset chunk and entity cache?
        }
        CACHE.getPlayerCache()
            .setGameMode(packet.getGamemode())
            .setLastDeathPos(packet.getLastDeathPos())
            .setPortalCooldown(packet.getPortalCooldown());
        CACHE.getChunkCache().updateCurrentDimension(packet);
        if (!packet.isKeepMetadata()) {
            CACHE.getPlayerCache().getThePlayer().getMetadata().clear();
        }
        if (!packet.isKeepAttributes()) {
            // todo: what do here?
        }
        MODULE_MANAGER.get(PlayerSimulation.class).handleRespawn();
        return true;
    }

    private void spectatorRespawn() {
        try {
            // load world and init self
            Proxy.getInstance().getSpectatorConnections().forEach(session -> {
                SpectatorUtils.initSpectator(session, () -> asList(CACHE.getChunkCache(), CACHE.getEntityCache(), CACHE.getMapDataCache(), session.getSpectatorPlayerCache()));
            });
        } finally {
            isSpectatorRespawning.set(false);
        }
    }
}
