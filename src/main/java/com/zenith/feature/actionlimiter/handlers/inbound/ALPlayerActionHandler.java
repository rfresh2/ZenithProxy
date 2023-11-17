package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALPlayerActionHandler implements IncomingHandler<ServerboundPlayerActionPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundPlayerActionPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowBlockBreaking && (
            packet.getAction() == PlayerAction.START_DIGGING
            || packet.getAction() == PlayerAction.FINISH_DIGGING
            || packet.getAction() == PlayerAction.CANCEL_DIGGING
        )) {
            // todo: force client to cancel digging and send block update packet
            return false;
        }
        if (!CONFIG.client.extra.actionLimiter.allowInventory && (
            packet.getAction() == PlayerAction.DROP_ITEM
            || packet.getAction() == PlayerAction.DROP_ITEM_STACK
            || packet.getAction() == PlayerAction.SWAP_HANDS
        )) {
            return false;
        }
        return true;
    }
}
