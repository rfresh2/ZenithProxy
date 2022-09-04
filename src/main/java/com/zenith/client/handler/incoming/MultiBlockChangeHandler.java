package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

public class MultiBlockChangeHandler implements HandlerRegistry.AsyncIncomingHandler<ServerMultiBlockChangePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerMultiBlockChangePacket packet, @NonNull ClientSession session) {
        for (BlockChangeRecord record : packet.getRecords())    {
            BlockChangeHandler.handleChange(record);
        }
        return true;
    }

    @Override
    public Class<ServerMultiBlockChangePacket> getPacketClass() {
        return ServerMultiBlockChangePacket.class;
    }
}
