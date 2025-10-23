package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;

import static com.zenith.Globals.CACHE;

public class PostOutgoingPlayerAbilitiesHandler implements PostOutgoingPacketHandler<ServerboundPlayerAbilitiesPacket, ClientSession> {
    @Override
    public void accept(final ServerboundPlayerAbilitiesPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().setFlying(packet.isFlying());
    }
}
