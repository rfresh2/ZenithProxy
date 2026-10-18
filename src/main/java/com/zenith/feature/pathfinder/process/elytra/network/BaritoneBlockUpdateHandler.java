package com.zenith.feature.pathfinder.process.elytra.network;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;

import static com.zenith.Globals.BARITONE;

public class BaritoneBlockUpdateHandler implements ClientEventLoopPacketHandler<ClientboundBlockUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundBlockUpdatePacket packet, final ClientSession session) {
        BARITONE.getElytraProcess().onBlockChange(packet.getEntry().getX(), packet.getEntry().getY(), packet.getEntry().getZ(), packet.getEntry().getBlock());
        return true;
    }
}
