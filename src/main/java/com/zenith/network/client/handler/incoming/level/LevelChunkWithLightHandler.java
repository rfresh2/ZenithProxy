package com.zenith.network.client.handler.incoming.level;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;

import static com.zenith.Shared.CACHE;

public class LevelChunkWithLightHandler implements ClientEventLoopPacketHandler<ClientboundLevelChunkWithLightPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundLevelChunkWithLightPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().add(packet);
        return true;
    }
}
