package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;

import static com.zenith.Shared.CONFIG;

public class ALClientCommandHandler implements PacketHandler<ServerboundClientCommandPacket, ServerConnection> {
    @Override
    public ServerboundClientCommandPacket apply(final ServerboundClientCommandPacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowRespawn)
            return packet;
        session.disconnect("ActionLimiter: Respawn not allowed");
        return null;
    }
}
