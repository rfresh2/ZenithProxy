package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.CACHE;

public class PlayerAbilitiesHandler implements AsyncPacketHandler<ClientboundPlayerAbilitiesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundPlayerAbilitiesPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setInvincible(packet.isInvincible());
        CACHE.getPlayerCache().setCanFly(packet.isCanFly());
        CACHE.getPlayerCache().setFlying(packet.isFlying());
        CACHE.getPlayerCache().setCreative(packet.isCreative());
        CACHE.getPlayerCache().setFlySpeed(packet.getFlySpeed());
        CACHE.getPlayerCache().setWalkSpeed(packet.getWalkSpeed());
        return true;
    }
}
