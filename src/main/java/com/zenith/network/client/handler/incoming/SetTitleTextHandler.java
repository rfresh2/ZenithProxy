package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.util.ComponentSerializer;

import java.util.Optional;

import static com.zenith.Shared.*;

public class SetTitleTextHandler implements AsyncIncomingHandler<ClientboundSetTitleTextPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetTitleTextPacket packet, final ClientSession session) {
        if (CONFIG.client.server.address.contains("2b2t.org")) {
            parse2bQueuePos(packet, session);
        }
        return true;
    }

    @Override
    public Class<ClientboundSetTitleTextPacket> getPacketClass() {
        return ClientboundSetTitleTextPacket.class;
    }

    private void parse2bQueuePos(ClientboundSetTitleTextPacket serverTitlePacket, final ClientSession session) {
        try {
            Optional<Integer> position = Optional.of(serverTitlePacket)
                .map(title -> ComponentSerializer.toRawString(title.getText()))
                .map(text -> {
                    String[] split = text.split(":");
                    if (split.length > 1) {
                        return split[1].trim();
                    } else {
                        return ""+Integer.MAX_VALUE; // some arbitrarily non-zero value
                    }
                })
                .map(Integer::parseInt);
            if (position.isPresent()) {
                if (position.get() != session.getLastQueuePosition()) {
                    EVENT_BUS.postAsync(new QueuePositionUpdateEvent(position.get()));
                }
                session.setLastQueuePosition(position.get());
            }
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing queue position from title packet", e);
        }
    }
}
