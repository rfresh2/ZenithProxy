package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.PrioStatusEvent;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.StartQueueEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class TabListDataHandler implements AsyncPacketHandler<ClientboundTabListPacket, ClientSession> {
    private Optional<Duration> queueDuration = Optional.empty();

    @Override
    public boolean applyAsync(@NonNull ClientboundTabListPacket packet, @NonNull ClientSession session) {
        CACHE.getTabListCache()
            .setHeader(packet.getHeader())
            .setFooter(packet.getFooter());
        if (Proxy.getInstance().isOn2b2t()) {
            parse2bQueueState(packet, session);
            if (session.isInQueue()) {
                parse2bPrioQueueState(packet);
            } else if (session.isOnline()) {
                parse2bPing(packet, session);
            }
        } else {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.post(new PlayerOnlineEvent());
            }
        }
        return true;
    }

    private synchronized void parse2bQueueState(ClientboundTabListPacket packet, ClientSession session) {
        Optional<String> queueHeader = Arrays.stream(ComponentSerializer.serializePlain(packet.getHeader()).split("\\\\n"))
                .map(String::trim)
                .map(m -> m.toLowerCase(Locale.ROOT))
                .filter(m -> m.contains("2b2t is full") || m.contains("pending") || m.contains("in queue"))
                .findAny();
        if (queueHeader.isPresent()) {
            if (!session.isInQueue()) {
                EVENT_BUS.postAsync(new StartQueueEvent());
                queueDuration = Optional.empty();
            }
            session.setInQueue(true);
            session.setOnline(false);
        } else if (session.isInQueue()) {
            session.setInQueue(false);
            session.setLastQueuePosition(Integer.MAX_VALUE);
            // need to calculate and store duration here as proxy connect time gets reset in the queue complete event handler
            queueDuration = Optional.of(Duration.between(Proxy.getInstance().getConnectTime(), Instant.now()));
            EVENT_BUS.postAsync(new QueueCompleteEvent(queueDuration.get()));
        } else if (!session.isOnline()) {
            session.setOnline(true);
            EVENT_BUS.postAsync(new PlayerOnlineEvent(queueDuration));
            queueDuration = Optional.empty();
        }
    }

    private void parse2bPrioQueueState(final ClientboundTabListPacket packet) {
        Optional.of(packet.getFooter())
                .map(ComponentSerializer::serializePlain)
                .map(textRaw -> textRaw.replace("\n", ""))
                .filter(messageString -> messageString.contains("priority"))
                .ifPresent(messageString -> {
                    /**
                     * non prio:
                     * "You can purchase priority queue status to join the server faster, visit shop.2b2t.org"
                     *
                     * prio:
                     * "This account has priority status and will be placed in a shorter queue."
                     */
                    EVENT_BUS.postAsync(new PrioStatusEvent(!messageString.contains("shop.2b2t.org")));
                });
    }

    private synchronized void parse2bPing(final ClientboundTabListPacket packet, ClientSession session) {
        Optional.of(packet.getFooter())
                .map(ComponentSerializer::serializePlain)
                .map(textRaw -> textRaw.replace("\n", ""))
                .map(String::trim)
                .filter(textRaw -> textRaw.contains("ping"))
                .ifPresent(line -> {
                    final List<String> hyphenSplit = Arrays.asList(line.split("—"));
                    if (!hyphenSplit.isEmpty()) {
                        // " XX ping"
                        final String pingSection = hyphenSplit.get(hyphenSplit.size() - 1);
                        final List<String> pingSectionSpaceSplit = Arrays.asList(pingSection.split(" "));
                        if (!pingSectionSpaceSplit.isEmpty()) {
                            final String ping = pingSectionSpaceSplit.get(1);
                            try {
                                int pingInt = Integer.parseInt(ping);
                                session.setPing(pingInt);
                    } catch (final Exception e) {
                        // f
                    }
                }
            }
        });
    }
}
