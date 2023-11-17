package com.zenith.feature.actionlimiter.handlers.inbound;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CONFIG;

public class ALInteractHandler implements IncomingHandler<ServerboundInteractPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundInteractPacket packet, final ServerConnection session) {
        if (!CONFIG.client.extra.actionLimiter.allowInteract) return false;
        return true;
    }
}
