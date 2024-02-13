package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.module.impl.ActionLimiter;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;

public class ALContainerClickHandler implements PacketHandler<ServerboundContainerClickPacket, ServerConnection> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ServerConnection session) {
        if (MODULE_MANAGER.get(ActionLimiter.class).bypassesLimits(session)) return packet;
        if (!CONFIG.client.extra.actionLimiter.allowInventory) return null;
        return packet;
    }
}
