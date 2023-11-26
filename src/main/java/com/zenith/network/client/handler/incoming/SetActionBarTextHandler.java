package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.util.ComponentSerializer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.zenith.Shared.*;

public class SetActionBarTextHandler implements AsyncPacketHandler<ClientboundSetActionBarTextPacket, ClientSession> {
    private Instant lastRestartEvent = Instant.EPOCH;

    @Override
    public boolean applyAsync(final ClientboundSetActionBarTextPacket packet, final ClientSession session) {
        if (CONFIG.client.server.address.contains("2b2t.org")) {
            parse2bRestart(packet, session);
        }
        return true;
    }

    private void parse2bRestart(ClientboundSetActionBarTextPacket serverTitlePacket, final ClientSession session) {
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
}
