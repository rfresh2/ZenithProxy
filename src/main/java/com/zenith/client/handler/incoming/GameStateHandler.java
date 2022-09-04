package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

public class GameStateHandler implements HandlerRegistry.AsyncIncomingHandler<ServerNotifyClientPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerNotifyClientPacket packet, @NonNull ClientSession session) {
        if (packet.getNotification() == ClientNotification.CHANGE_GAMEMODE) {
            CACHE.getPlayerCache().setGameMode((GameMode) packet.getValue());
        }
        return true;
    }

    @Override
    public Class<ServerNotifyClientPacket> getPacketClass() {
        return ServerNotifyClientPacket.class;
    }
}
