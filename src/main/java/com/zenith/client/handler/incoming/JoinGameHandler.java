package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class JoinGameHandler implements HandlerRegistry.IncomingHandler<ServerJoinGamePacket, ClientSession> {
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
        return true;
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
