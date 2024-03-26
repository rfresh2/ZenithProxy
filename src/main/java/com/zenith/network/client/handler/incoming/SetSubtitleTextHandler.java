package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import com.zenith.util.ComponentSerializer;

import java.util.Optional;

import static com.zenith.Shared.CLIENT_LOG;
import static com.zenith.Shared.EVENT_BUS;

public class SetSubtitleTextHandler implements ClientEventLoopPacketHandler<ClientboundSetSubtitleTextPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ClientboundSetSubtitleTextPacket packet, final ClientSession session) {
        if (Proxy.getInstance().isInQueue()) {
            parse2bQueuePos(packet, session);
        }
        return true;
    }

    private void parse2bQueuePos(ClientboundSetSubtitleTextPacket serverTitlePacket, final ClientSession session) {
        try {
            Optional<Integer> position = Optional.of(serverTitlePacket)
                .map(title -> ComponentSerializer.serializePlain(title.getText()))
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
