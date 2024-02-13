package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.zenith.module.impl.ActionLimiter;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;

public class ALPlayerActionHandler implements PacketHandler<ServerboundPlayerActionPacket, ServerConnection> {
    @Override
    public ServerboundPlayerActionPacket apply(final ServerboundPlayerActionPacket packet, final ServerConnection session) {
        if (MODULE_MANAGER.get(ActionLimiter.class).bypassesLimits(session)) return packet;
        if (!CONFIG.client.extra.actionLimiter.allowBlockBreaking && (
            packet.getAction() == PlayerAction.START_DIGGING
            || packet.getAction() == PlayerAction.FINISH_DIGGING
            || packet.getAction() == PlayerAction.CANCEL_DIGGING
        )) {
            // todo: force client to cancel digging and send block update packet
            return null;
        }
        if (!CONFIG.client.extra.actionLimiter.allowInventory && (
            packet.getAction() == PlayerAction.DROP_ITEM
            || packet.getAction() == PlayerAction.DROP_ITEM_STACK
            || packet.getAction() == PlayerAction.SWAP_HANDS
        )) {
            return null;
        }
        return packet;
    }
}
