package com.zenith.feature.pathfinder.process.elytra.network;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;

import static com.zenith.Globals.BARITONE;

public class BaritoneSectionBlocksUpdateHandler implements ClientEventLoopPacketHandler<ClientboundSectionBlocksUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSectionBlocksUpdatePacket packet, final ClientSession session) {
        for (var e : packet.getEntries()) {
            BARITONE.getElytraProcess().onBlockChange(e.getX(), e.getY(), e.getZ(), e.getBlock());
        }
        return true;
    }
}
