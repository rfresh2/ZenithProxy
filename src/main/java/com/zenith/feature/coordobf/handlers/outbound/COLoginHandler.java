package com.zenith.feature.coordobf.handlers.outbound;

import com.zenith.Proxy;
import com.zenith.module.impl.CoordObfuscation;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.MODULE;

public class COLoginHandler implements PacketHandler<ClientboundLoginPacket, ServerSession> {
    @Override
    public ClientboundLoginPacket apply(final ClientboundLoginPacket packet, final ServerSession session) {
        var coordObf = MODULE.get(CoordObfuscation.class);
        if (coordObf.getPlayerState(session).isInGame()) {
            // i.e. velocity world switching
            coordObf.disconnect(session, "World switching");
            return null;
        }
        if (Proxy.getInstance().isOn2b2t())
            if (!Proxy.getInstance().getClient().isOnline()
                || Proxy.getInstance().isInQueue()
                || CACHE.getPlayerCache().getGameMode() == GameMode.SPECTATOR
            ) {
                // prevent queue from leaking the offset
                coordObf.disconnect(session, "Queueing");
                return null;
            }
        return new ClientboundLoginPacket(
            packet.getEntityId(),
            packet.isHardcore(),
            packet.getWorldNames(),
            packet.getMaxPlayers(),
            packet.getViewDistance(),
            packet.getSimulationDistance(),
            packet.isReducedDebugInfo(),
            packet.isEnableRespawnScreen(),
            packet.isDoLimitedCrafting(),
            new PlayerSpawnInfo(
                packet.getCommonPlayerSpawnInfo().getDimension(),
                packet.getCommonPlayerSpawnInfo().getWorldName(),
                packet.getCommonPlayerSpawnInfo().getHashedSeed(),
                packet.getCommonPlayerSpawnInfo().getGameMode(),
                packet.getCommonPlayerSpawnInfo().getPreviousGamemode(),
                packet.getCommonPlayerSpawnInfo().isDebug(),
                packet.getCommonPlayerSpawnInfo().isFlat(),
                null,
                packet.getCommonPlayerSpawnInfo().getPortalCooldown(),
                packet.getCommonPlayerSpawnInfo().getSeaLevel()
            ),
            packet.isOnlineMode(),
            false
        );
    }
}
