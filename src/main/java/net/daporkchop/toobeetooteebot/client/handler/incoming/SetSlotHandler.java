package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class SetSlotHandler implements HandlerRegistry.IncomingHandler<ServerSetSlotPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerSetSlotPacket packet, @NonNull PorkClientSession session) {
        if (packet.getWindowId() == 0 && packet.getSlot() >= 0) {
            CACHE.getPlayerCache().getInventory()[packet.getSlot()] = packet.getItem();
        }
        return true;
    }

    @Override
    public Class<ServerSetSlotPacket> getPacketClass() {
        return ServerSetSlotPacket.class;
    }
}
