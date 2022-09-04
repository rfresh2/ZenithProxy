package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.zenith.client.ClientSession;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.*;

public class PlayerHealthHandler implements HandlerRegistry.AsyncIncomingHandler<ServerPlayerHealthPacket, ClientSession> {

    @Override
    public boolean applyAsync(@NonNull ServerPlayerHealthPacket packet, @NonNull ClientSession session) {
        if (packet.getHealth() != CACHE.getPlayerCache().getThePlayer().getHealth()) {
            MODULE_EXECUTOR_SERVICE.execute(() -> EVENT_BUS.dispatch(
                    new PlayerHealthChangedEvent(packet.getHealth(), CACHE.getPlayerCache().getThePlayer().getHealth())));
        }

        CACHE.getPlayerCache().getThePlayer()
                .setFood(packet.getFood())
                .setSaturation(packet.getSaturation())
                .setHealth(packet.getHealth());
        CACHE_LOG.debug("Player food: {}", packet.getFood());
        CACHE_LOG.debug("Player saturation: {}", packet.getSaturation());
        CACHE_LOG.debug("Player health: {}", packet.getHealth());
        return true;
    }

    @Override
    public Class<ServerPlayerHealthPacket> getPacketClass() {
        return ServerPlayerHealthPacket.class;
    }
}
