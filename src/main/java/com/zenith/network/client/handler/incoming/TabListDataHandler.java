package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.PrioStatusEvent;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.StartQueueEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;
import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.zenith.Shared.*;

public class TabListDataHandler implements AsyncIncomingHandler<ServerPlayerListDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerPlayerListDataPacket packet, @NonNull ClientSession session) {
        CACHE.getTabListCache().getTabList()
                .setHeader(packet.getHeader())
                .setFooter(packet.getFooter());
        if (CONFIG.client.server.address.toLowerCase(Locale.ROOT).contains("2b2t.org")) {
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

    @Override
    public Class<ServerPlayerListDataPacket> getPacketClass() {
        return ServerPlayerListDataPacket.class;
    }

    private synchronized void parse2bQueueState(ServerPlayerListDataPacket packet, ClientSession session) {
        Optional<String> queueHeader = Arrays.stream(packet.getHeader().split("\\\\n"))
                .map(String::trim)
                .map(m -> m.toLowerCase(Locale.ROOT))
                .filter(m -> m.contains("2b2t is full") || m.contains("pending") || m.contains("in queue"))
                .findAny();
        if (queueHeader.isPresent()) {
            if (!session.isInQueue()) {
                EVENT_BUS.postAsync(new StartQueueEvent());
            }
            session.setInQueue(true);
            session.setOnline(false);
        } else if (session.isInQueue()) {
            session.setInQueue(false);
            session.setLastQueuePosition(Integer.MAX_VALUE);
            EVENT_BUS.postAsync(new QueueCompleteEvent());
        } else if (!session.isOnline()) {
            session.setOnline(true);
            EVENT_BUS.postAsync(new PlayerOnlineEvent());
        }
    }

    private void parse2bPrioQueueState(final ServerPlayerListDataPacket packet) {
        parse2b2tTablistFooter(packet.getFooter())
                .map(TextComponent::toRawString)
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

    private synchronized void parse2bPing(final ServerPlayerListDataPacket packet, ClientSession session) {
        parse2b2tTablistFooter(packet.getFooter())
                .map(TextComponent::toRawString)
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

    private Optional<MCTextRoot> parse2b2tTablistFooter(final String footer) {
        try {
            return Optional.of(FORMAT_PARSER.parse(footer));
        } catch (final Exception e) {
            CLIENT_LOG.debug("Error parsing 2b2t tablist footer", e);
            return Optional.empty();
        }
    }
}
