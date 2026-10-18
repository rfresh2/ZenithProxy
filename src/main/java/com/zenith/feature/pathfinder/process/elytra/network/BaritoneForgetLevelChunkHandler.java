package com.zenith.feature.pathfinder.process.elytra.network;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;

import static com.zenith.Globals.BARITONE;

public class BaritoneForgetLevelChunkHandler implements ClientEventLoopPacketHandler<ClientboundForgetLevelChunkPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundForgetLevelChunkPacket packet, final ClientSession session) {
        BARITONE.getElytraProcess().onChunkUnload(packet.getX(), packet.getZ());
        return true;
    }
}
