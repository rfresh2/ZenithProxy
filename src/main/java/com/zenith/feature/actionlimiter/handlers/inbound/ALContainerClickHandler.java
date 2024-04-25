package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Shared.CONFIG;

public class ALContainerClickHandler implements PacketHandler<ServerboundContainerClickPacket, ServerConnection> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowInventory) return null;
        return packet;
    }
}
