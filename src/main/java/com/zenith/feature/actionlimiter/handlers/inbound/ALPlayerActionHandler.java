package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import static com.zenith.Shared.CONFIG;

public class ALPlayerActionHandler implements PacketHandler<ServerboundPlayerActionPacket, ServerConnection> {
    @Override
    public ServerboundPlayerActionPacket apply(final ServerboundPlayerActionPacket packet, final ServerConnection session) {
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
