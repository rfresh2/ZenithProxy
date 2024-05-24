package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;

import static com.zenith.Shared.CACHE;

public class PlayerAbilitiesHandler implements ClientEventLoopPacketHandler<ClientboundPlayerAbilitiesPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundPlayerAbilitiesPacket packet, final ClientSession session) {
        CACHE.getPlayerCache()
            .setInvincible(packet.isInvincible())
            .setCanFly(packet.isCanFly())
            .setFlying(packet.isFlying())
            .setCreative(packet.isCreative())
            .setFlySpeed(packet.getFlySpeed())
            .setWalkSpeed(packet.getWalkSpeed());
        return true;
    }
}
