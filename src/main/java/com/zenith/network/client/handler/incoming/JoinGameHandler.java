package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.IncomingHandler;
import lombok.NonNull;

import java.util.Locale;

import static com.zenith.Shared.*;

public class JoinGameHandler implements IncomingHandler<ServerJoinGamePacket, ClientSession> {
    @Override
    public boolean apply(@NonNull ServerJoinGamePacket packet, @NonNull ClientSession session) {
        CACHE.getPlayerCache()
                .setEntityId(packet.getEntityId())
                .setDimension(packet.getDimension())
                .setWorldType(packet.getWorldType())
                .setGameMode(packet.getGameMode())
                .setDifficulty(packet.getDifficulty())
                .setHardcore(packet.getHardcore())
                .setMaxPlayers(packet.getMaxPlayers())
                .setReducedDebugInfo(packet.getReducedDebugInfo());

        session.send(new ClientSettingsPacket(
                "en_US",
                // todo: maybe set this to a config.
                //  or figure out how we don't overwrite this for clients when they connect due to metadata cache
                25,
                ChatVisibility.FULL,
                true,
                SkinPart.values(),
                Hand.OFF_HAND
        ));
        if (!CONFIG.client.server.address.toLowerCase(Locale.ROOT).endsWith("2b2t.org")) {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.dispatch(new PlayerOnlineEvent());
            }
        }
        return true;
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
