package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundEditBookPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALEditBookHandler implements PacketHandler<ServerboundEditBookPacket, ServerConnection> {
    @Override
    public ServerboundEditBookPacket apply(final ServerboundEditBookPacket packet, final ServerConnection session) {
        if(!CONFIG.client.extra.actionLimiter.allowBookSigning
            // if title is not null, the book is being signed
            && packet.getTitle() != null) {
            return new ServerboundEditBookPacket(
                packet.getSlot(),
                packet.getPages(),
                null // edits the book but does not sign it
            );
        }
        return packet;
    }
}
