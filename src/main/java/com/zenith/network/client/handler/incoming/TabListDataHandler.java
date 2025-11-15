package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.event.client.ClientOnlineEvent;
import com.zenith.event.client.PrioStatusEvent;
import com.zenith.event.queue.QueueCompleteEvent;
import com.zenith.event.queue.QueueStartEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.zenith.Globals.*;
import static com.zenith.util.math.MathHelper.formatDuration;

public class TabListDataHandler implements ClientEventLoopPacketHandler<ClientboundTabListPacket, ClientSession> {
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
//                parse2bPing(packet, session);
            }
        } else {
            if (!session.isOnline()) {
                session.setOnline(true);
                EVENT_BUS.post(new ClientOnlineEvent());
            }
        }
        return true;
    }

    private synchronized void parse2bQueueState(ClientboundTabListPacket packet, ClientSession session) {
        final String[] headerContentLineBreakSplit = ComponentSerializer.serializePlain(packet.getHeader()).split("\\\\n");
        Optional<String> queueHeader = Arrays.stream(headerContentLineBreakSplit)
                .map(String::trim)
                .map(m -> m.toLowerCase(Locale.ROOT))
                .filter(m -> m.contains("2b2t is full") || m.contains("pending") || m.contains("in queue"))
                .findAny();
        if (queueHeader.isPresent()) {
            if (!session.isInQueue()) {
                boolean wasOnline = false;
                Duration wasOnlineDuration = Duration.ZERO;
                if (session.isWasOnline()) {
                    // can occur if we get kicked to queue in certain cases like if the main server restarts
                    // resetting connect time to calculate queue duration correctly
                    wasOnline = true;
                    wasOnlineDuration = Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds());
                    CLIENT_LOG.info("Kicked to queue. Online duration: {}", formatDuration(wasOnlineDuration));
                }
                EVENT_BUS.postAsync(new QueueStartEvent(wasOnline, wasOnlineDuration));
                queueDuration = Optional.empty();
            }
            session.setInQueue(true);
            session.setOnline(false);
        } else if (session.isInQueue()) {
            session.setInQueue(false);
            session.setLastQueuePosition(Integer.MAX_VALUE);
            // need to calculate and store duration here as proxy connect time gets reset in the queue complete event handler
            queueDuration = Optional.of(Duration.ofSeconds(Proxy.getInstance().getOnlineTimeSeconds()));
            EVENT_BUS.postAsync(new QueueCompleteEvent(queueDuration.get()));
        } else if (!session.isOnline()) {
            if (headerContentLineBreakSplit.length == 1 && headerContentLineBreakSplit[0].isEmpty()) return; // can occur right after game profile packet received
            session.setOnline(true);
            EVENT_BUS.postAsync(new ClientOnlineEvent(queueDuration));
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
                    if (messageString.contains("purchase priority queue")) {
                        EVENT_BUS.postAsync(new PrioStatusEvent(false));
                    } else if (messageString.contains("has priority status")) {
                        EVENT_BUS.postAsync(new PrioStatusEvent(true));
                    }
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
                        final String pingSection = hyphenSplit.getLast();
                        final List<String> pingSectionSpaceSplit = Arrays.asList(pingSection.split(" "));
                        if (!pingSectionSpaceSplit.isEmpty()) {
                            try {
                                session.setPing(Integer.parseInt(pingSectionSpaceSplit.get(1)));
                            } catch (final Exception e) {
                                // f
                            }
                        }
                    }
                });
    }
}
