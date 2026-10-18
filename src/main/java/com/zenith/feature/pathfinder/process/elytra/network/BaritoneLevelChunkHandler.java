package com.zenith.feature.pathfinder.process.elytra.network;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;

import static com.zenith.Globals.BARITONE;
import static com.zenith.Globals.CACHE;

public class BaritoneLevelChunkHandler implements ClientEventLoopPacketHandler<ClientboundLevelChunkWithLightPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundLevelChunkWithLightPacket packet, final ClientSession session) {
        var chunk = CACHE.getChunkCache().get(packet.getX(), packet.getZ());
        if (chunk != null) {
            BARITONE.getElytraProcess().onChunkEvent(chunk);
        }
        return true;
    }
}
