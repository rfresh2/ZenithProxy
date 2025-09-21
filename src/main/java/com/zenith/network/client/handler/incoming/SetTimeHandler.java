package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import static com.zenith.Globals.CACHE;

public class SetTimeHandler implements ClientEventLoopPacketHandler<ClientboundSetTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundSetTimePacket packet, ClientSession session) {
        CACHE.getChunkCache().updateWorldTime(packet);
        return true;
    }
}
