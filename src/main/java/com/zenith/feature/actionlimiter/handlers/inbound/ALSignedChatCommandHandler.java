package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Shared.CONFIG;

public class ALSignedChatCommandHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerConnection> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerConnection session) {
        if (CONFIG.client.extra.actionLimiter.allowChat) return packet;
        else return null;
    }
}
