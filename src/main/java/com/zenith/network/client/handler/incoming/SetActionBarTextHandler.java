package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.event.server.ServerRestartingEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import com.zenith.event.queue.QueuePositionUpdateEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zenith.Globals.CLIENT_LOG;
import static com.zenith.Globals.EVENT_BUS;

public class SetActionBarTextHandler implements ClientEventLoopPacketHandler<ClientboundSetActionBarTextPacket, ClientSession> {
    private Instant lastRestartEvent = Instant.EPOCH;

    @Override
    public boolean applyAsync(final ClientboundSetActionBarTextPacket packet, final ClientSession session) {
        if (Proxy.getInstance().isOn2b2t()) parse2bRestart(packet, session);
        if (Proxy.getInstance().isInQueue()) parse2bQueuePos(packet, session);
        return true;
    }

    private void parse2bRestart(ClientboundSetActionBarTextPacket serverTitlePacket, final ClientSession session) {
        try {
            Optional.of(serverTitlePacket)
                .map(title -> ComponentSerializer.serializePlain(title.getText()))
                .filter(text -> text.toLowerCase().contains("restart"))
                .ifPresent(text -> {
                    if (lastRestartEvent.isBefore(Instant.now().minus(1, ChronoUnit.MINUTES))) {
                        lastRestartEvent = Instant.now();
                        EVENT_BUS.postAsync(new ServerRestartingEvent(text));
                    }
                });
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing restart message from title packet", e);
        }
    }

    private void parse2bQueuePos(ClientboundSetActionBarTextPacket packet, final ClientSession session) {
        try {
            Optional<Integer> position = Optional.of(packet)
                .map(p -> ComponentSerializer.serializePlain(p.getText()))
                .map(text -> {
                    String t = text.trim();
                    Matcher m1 = Pattern.compile("(?i)position\\s*(?:in\\s*queue)?\\s*:?\\s*(\\d+)").matcher(t);
                    if (m1.find()) return m1.group(1);
                    Matcher m2 = Pattern.compile("位置\\s*[:：]\\s*(\\d+)").matcher(t);
                    if (m2.find()) return m2.group(1);
                    Matcher m3 = Pattern.compile("(\\d+)\\s*/").matcher(t);
                    if (m3.find()) return m3.group(1);
                    return "" + Integer.MAX_VALUE;
                })
                .map(Integer::parseInt);
            if (position.isPresent()) {
                if (position.get() != session.getLastQueuePosition()) {
                    EVENT_BUS.postAsync(new QueuePositionUpdateEvent(position.get()));
                }
                session.setLastQueuePosition(position.get());
            }
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing queue position from action bar packet", e);
        }
    }
}
