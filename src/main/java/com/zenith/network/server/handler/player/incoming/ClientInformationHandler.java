package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;

import static com.zenith.Shared.CACHE;

public class ClientInformationHandler implements AsyncPacketHandler<ServerboundClientInformationPacket, ServerConnection> {
    public static final ClientInformationHandler INSTANCE = new ClientInformationHandler();
    @Override
    public boolean applyAsync(ServerboundClientInformationPacket packet, ServerConnection session) {
        CACHE.getChunkCache().setRenderDistance(packet.getRenderDistance());
        return true;
    }
}
