package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

import static com.zenith.Shared.CONFIG;

public class ALUseItemHandler implements PacketHandler<ServerboundUseItemPacket, ServerConnection> {
    @Override
    public ServerboundUseItemPacket apply(final ServerboundUseItemPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowUseItem) return null;
        return packet;
    }
}
