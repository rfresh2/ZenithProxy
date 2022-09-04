package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CLIENT_LOG;

public class BlockChangeHandler implements HandlerRegistry.AsyncIncomingHandler<ServerBlockChangePacket, ClientSession> {
    static boolean handleChange(@NonNull BlockChangeRecord record) {
        try {
            CLIENT_LOG.debug("Handling block update: pos: [" + record.getPosition().getX() + ", " + record.getPosition().getY() + ", " + record.getPosition().getZ() + "], id: " + record.getBlock().getId() + ", data: " + record.getBlock().getData());
            CACHE.getChunkCache().updateBlock(new ServerBlockChangePacket(record));
        } catch (final Exception e) {
            CLIENT_LOG.error("Error applying block update", e);
        }
        return true;
    }

    @Override
    public boolean applyAsync(@NonNull ServerBlockChangePacket packet, @NonNull ClientSession session) {
        return handleChange(packet.getRecord());
    }

    @Override
    public Class<ServerBlockChangePacket> getPacketClass() {
        return ServerBlockChangePacket.class;
    }
}
