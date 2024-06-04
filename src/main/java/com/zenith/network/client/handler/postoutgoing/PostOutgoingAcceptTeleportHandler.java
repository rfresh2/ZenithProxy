package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

import java.util.NoSuchElementException;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.CLIENT_LOG;

public class PostOutgoingAcceptTeleportHandler implements ClientEventLoopPacketHandler<ServerboundAcceptTeleportationPacket, ClientSession> {

    @Override
    public boolean applyAsync(final ServerboundAcceptTeleportationPacket packet, final ClientSession session) {
        try {
            var queue = CACHE.getPlayerCache().getTeleportQueue();
            int expectedTeleportId = queue.firstInt();
            if (packet.getId() == expectedTeleportId) {
                CACHE.getPlayerCache().getTeleportQueue().dequeueInt();
            } else {
                CLIENT_LOG.debug("Accepting out-of-order teleport ID: expected: {}, actual: {}", expectedTeleportId, packet.getId());
                // possible we still have this teleport id in our queue and things may go very wrong here
                // could also occur as a race condition at player login in the sequence documented below
            }
        } catch (final NoSuchElementException e) {
            // this will always occur at controlling player login
            // we send them a teleport packet with a random ID during their login
            // ideally we would cancel the player's accept packet but seems to be fine if we send it through anyway
            CLIENT_LOG.debug("Accepting unqueued teleport ID: {}", packet.getId());
        }
        return true;
    }
}
