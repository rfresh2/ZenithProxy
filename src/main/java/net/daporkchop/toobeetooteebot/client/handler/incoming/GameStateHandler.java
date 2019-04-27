package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class GameStateHandler implements HandlerRegistry.IncomingHandler<ServerNotifyClientPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerNotifyClientPacket packet, @NonNull PorkClientSession session) {
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
