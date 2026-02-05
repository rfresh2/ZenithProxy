package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;

import static com.zenith.Globals.CACHE;

public class OutgoingPlayerLoadedHandler implements PacketHandler<ServerboundPlayerLoadedPacket, ClientSession> {
    @Override
    public ServerboundPlayerLoadedPacket apply(final ServerboundPlayerLoadedPacket packet, final ClientSession session) {
        if (CACHE.getPlayerCache().isClientLoaded()) {
            return null;
        } else {
            CACHE.getPlayerCache().setClientLoaded(true);
            return packet;
        }
    }
}
