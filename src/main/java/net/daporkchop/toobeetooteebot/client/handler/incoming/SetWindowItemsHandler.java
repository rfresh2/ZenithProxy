package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

/**
 * @author DaPorkchop_
 */
public class SetWindowItemsHandler implements HandlerRegistry.IncomingHandler<ServerWindowItemsPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerWindowItemsPacket packet, @NonNull PorkClientSession session) {
        if (packet.getWindowId() == 0)  { //player inventory
            ItemStack[] dst = CACHE.getPlayerCache().getInventory();
            System.arraycopy(packet.getItems(), 0, dst, 0, dst.length);
        }
        return true;
    }

    @Override
    public Class<ServerWindowItemsPacket> getPacketClass() {
        return ServerWindowItemsPacket.class;
    }
}
