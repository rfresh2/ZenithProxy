package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import com.zenith.util.ComponentSerializer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.zenith.Shared.*;

public class SetSubtitleTextPacketHandler implements AsyncIncomingHandler<ClientboundSetSubtitleTextPacket, ClientSession> {
    private Instant lastRestartEvent = Instant.EPOCH;

    @Override
    public boolean applyAsync(ClientboundSetSubtitleTextPacket packet, ClientSession session) {
        if (CONFIG.client.server.address.contains("2b2t.org")) {
            parse2bRestart(packet, session);
            parse2bQueuePos(packet, session);
        }
        return true;
    }

    @Override
    public Class<ClientboundSetSubtitleTextPacket> getPacketClass() {
        return ClientboundSetSubtitleTextPacket.class;
    }

    private void parse2bRestart(ClientboundSetSubtitleTextPacket serverTitlePacket, final ClientSession session) {
        try {
            Optional.of(serverTitlePacket)
                .map(title -> ComponentSerializer.toRawString(title.getText()))
                .filter(text -> text.toLowerCase().contains("restart"))
                .ifPresent(text -> {
                    if (lastRestartEvent.isBefore(Instant.now().minus(15, ChronoUnit.MINUTES))) {
                        lastRestartEvent = Instant.now();
                        EVENT_BUS.postAsync(new ServerRestartingEvent(text));
                    }
                });
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing restart message from title packet", e);
        }
    }

    private void parse2bQueuePos(ClientboundSetSubtitleTextPacket serverTitlePacket, final ClientSession session) {
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
