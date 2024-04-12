package com.zenith.network.client.handler.incoming.spawn;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import com.zenith.util.math.MutableVec3i;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;

import static com.zenith.Shared.CACHE;

public class SpawnPositionHandler implements ClientEventLoopPacketHandler<ClientboundSetDefaultSpawnPositionPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSetDefaultSpawnPositionPacket packet, ClientSession session) {
        CACHE.getPlayerCache().setSpawnPosition(MutableVec3i.from(packet.getPosition()));
        return true;
    }
}
