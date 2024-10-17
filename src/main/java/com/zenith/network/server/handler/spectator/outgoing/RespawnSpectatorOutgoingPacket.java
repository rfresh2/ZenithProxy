package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;

public class RespawnSpectatorOutgoingPacket implements PacketHandler<ClientboundRespawnPacket, ServerSession> {
    @Override
    public ClientboundRespawnPacket apply(final ClientboundRespawnPacket packet, final ServerSession session) {
        return new ClientboundRespawnPacket(
            new PlayerSpawnInfo(
                packet.getCommonPlayerSpawnInfo().getDimension(),
                packet.getCommonPlayerSpawnInfo().getWorldName(),
                packet.getCommonPlayerSpawnInfo().getHashedSeed(),
                GameMode.SPECTATOR,
                GameMode.SPECTATOR,
                packet.getCommonPlayerSpawnInfo().isDebug(),
                packet.getCommonPlayerSpawnInfo().isFlat(),
                packet.getCommonPlayerSpawnInfo().getLastDeathPos(),
                packet.getCommonPlayerSpawnInfo().getPortalCooldown(),
                packet.getCommonPlayerSpawnInfo().getSeaLevel()
            ),
            packet.isKeepMetadata(),
            packet.isKeepAttributeModifiers()
        );
    }
}
