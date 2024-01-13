package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import lombok.NonNull;

import java.util.List;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class LoginHandler implements PacketHandler<ClientboundLoginPacket, ClientSession> {
    @Override
    public ClientboundLoginPacket apply(@NonNull ClientboundLoginPacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache()
            .setHardcore(packet.isHardcore())
            .setEntityId(packet.getEntityId())
            .setUuid(CACHE.getProfileCache().getProfile().getId())
            .setLastDeathPos(packet.getLastDeathPos())
            .setPortalCooldown(packet.getPortalCooldown())
            .setMaxPlayers(packet.getMaxPlayers())
            .setGameMode(packet.getGameMode())
            .setEnableRespawnScreen(packet.isEnableRespawnScreen())
            .setReducedDebugInfo(packet.isReducedDebugInfo());
        CACHE.getChunkCache().updateRegistryTag(packet.getRegistry());
        CACHE.getChunkCache().setCurrentWorld(
            packet.getDimension(),
            packet.getWorldName(),
            packet.getHashedSeed(),
            packet.isDebug(),
            packet.isFlat()
        );
        CACHE.getChunkCache().setServerViewDistance(packet.getViewDistance());
        CACHE.getChunkCache().setServerSimulationDistance(packet.getSimulationDistance());

        session.send(new ServerboundClientInformationPacket(
            "en_US",
            // todo: maybe set this to a config.
            //  or figure out how we don't overwrite this for clients when they connect due to metadata cache
            25,
            ChatVisibility.FULL,
            true,
            List.of(SkinPart.values()),
            HandPreference.RIGHT_HAND,
            false,
            false
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
