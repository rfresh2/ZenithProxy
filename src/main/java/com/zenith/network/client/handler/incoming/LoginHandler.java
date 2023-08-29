package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

import java.util.List;
import java.util.Locale;

import static com.zenith.Shared.*;

public class LoginHandler implements IncomingHandler<ClientboundLoginPacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ClientboundLoginPacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache()
            .setHardcore(packet.isHardcore())
            .setEntityId(packet.getEntityId())
            .setDimension(packet.getDimension())
            .setGameMode(packet.getGameMode())
            .setWorldNames(packet.getWorldNames())
            .setWorldName(packet.getWorldName())
            .setRegistryCodec(packet.getRegistry())
            .setHashedSeed(packet.getHashedSeed())
            .setViewDistance(packet.getViewDistance())
            .setSimulationDistance(packet.getSimulationDistance())
            .setEnableRespawnScreen(packet.isEnableRespawnScreen())
            .setDebug(packet.isDebug())
            .setFlat(packet.isFlat())
            .setLastDeathPos(packet.getLastDeathPos())
            .setPortalCooldown(packet.getPortalCooldown())
            .setMaxPlayers(packet.getMaxPlayers());

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
        if (!CONFIG.client.server.address.toLowerCase(Locale.ROOT).endsWith("2b2t.org")) {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.post(new PlayerOnlineEvent());
            }
        }
        return true;
    }

    @Override
    public Class<ClientboundLoginPacket> getPacketClass() {
        return ClientboundLoginPacket.class;
    }
}
