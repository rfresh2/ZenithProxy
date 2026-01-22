package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;

import static com.zenith.Globals.CACHE;

public class PingHandler implements ClientEventLoopPacketHandler<ClientboundPingPacket, ClientSession> {
    public static final PingHandler INSTANCE = new PingHandler();
    @Override
    public boolean applyAsync(final ClientboundPingPacket packet, final ClientSession session) {
        CACHE.getPlayerCache().getPingQueue().add(new PlayerCache.PingRequest(System.nanoTime(), packet.getId()));
        // grim ac uses this to determine leniency in player movements. should be synced to actual ping from player
        if (!Proxy.getInstance().hasActivePlayer()) {
            // race condition may be possible here causing a pong to be lost
            // 1. this packet is enqueued in the event loop
            // 2. controlling player enters the logged in state
            // 3. ping handler is executed and pong is now not sent by either the player or this handler
            // this is solved by the ping queue and handlers
            session.sendAsync(new ServerboundPongPacket(packet.getId()));
        }
        return true;
    }
}
