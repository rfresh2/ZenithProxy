package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.cache.CacheResetType;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import java.util.List;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;
import static java.util.Arrays.asList;

public class LoginHandler implements PacketHandler<ClientboundLoginPacket, ClientSession> {
    @Override
    public ClientboundLoginPacket apply(@NonNull ClientboundLoginPacket packet, @NonNull ClientSession session) {
        CACHE.reset(CacheResetType.LOGIN);
        CACHE.getSectionCountProvider().updateDimension(packet.getCommonPlayerSpawnInfo());
        CACHE.getPlayerCache()
            .setHardcore(packet.isHardcore())
            .setEntityId(packet.getEntityId())
            .setUuid(CACHE.getProfileCache().getProfile().getId())
            .setLastDeathPos(packet.getCommonPlayerSpawnInfo().getLastDeathPos())
            .setPortalCooldown(packet.getCommonPlayerSpawnInfo().getPortalCooldown())
            .setMaxPlayers(packet.getMaxPlayers())
            .setGameMode(packet.getCommonPlayerSpawnInfo().getGameMode())
            .setEnableRespawnScreen(packet.isEnableRespawnScreen())
            .setReducedDebugInfo(packet.isReducedDebugInfo());
        CACHE.getChunkCache().setWorldNames(asList(packet.getWorldNames()));
        CACHE.getChunkCache().setCurrentWorld(
            packet.getCommonPlayerSpawnInfo().getDimension(),
            packet.getCommonPlayerSpawnInfo().getWorldName(),
            packet.getCommonPlayerSpawnInfo().getHashedSeed(),
            packet.getCommonPlayerSpawnInfo().isDebug(),
            packet.getCommonPlayerSpawnInfo().isFlat()
        );
        CACHE.getChunkCache().setServerViewDistance(packet.getViewDistance());
        CACHE.getChunkCache().setServerSimulationDistance(packet.getSimulationDistance());

        session.send(new ServerboundClientInformationPacket(
            "en_US",
            25,
            ChatVisibility.FULL,
            true,
            List.of(SkinPart.values()),
            HandPreference.RIGHT_HAND,
            false,
            false,
            ParticleStatus.MINIMAL
        ));
        if (!Proxy.getInstance().isOn2b2t()) {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.post(new PlayerOnlineEvent());
            }
        }
        return packet;
    }
}
