package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;

public class RespawnSpectatorOutgoingPacket implements PacketHandler<ClientboundRespawnPacket, ServerConnection> {
    @Override
    public ClientboundRespawnPacket apply(final ClientboundRespawnPacket packet, final ServerConnection session) {
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
                packet.getCommonPlayerSpawnInfo().getPortalCooldown()
            ),
            packet.isKeepMetadata(),
            packet.isKeepAttributeModifiers()
        );
    }
}
