package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.TPS;

public class SetTimeHandler implements ClientEventLoopPacketHandler<ClientboundSetTimePacket, ClientSession> {

    @Override
    public boolean applyAsync(ClientboundSetTimePacket packet, ClientSession session) {
        CACHE.getChunkCache().updateWorldTime(packet);
        TPS.handleTimeUpdate();
        return true;
    }
}
