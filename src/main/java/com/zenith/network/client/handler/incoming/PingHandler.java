package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

public class PingHandler implements ClientEventLoopPacketHandler<ClientboundPingPacket, ClientSession> {
    public static PingHandler INSTANCE = new PingHandler();
    @Override
    public boolean applyAsync(final ClientboundPingPacket packet, final ClientSession session) {
        // grim ac uses this to determine leniency in player movements. should be synced to actual ping from player
        if (Proxy.getInstance().getCurrentPlayer().get() == null) {
            session.sendAsync(new ServerboundPongPacket(packet.getId()));
        }
        return true;
    }
}
