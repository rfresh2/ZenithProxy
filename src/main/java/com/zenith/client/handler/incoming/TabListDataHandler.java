package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.zenith.client.ClientSession;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.PrioStatusUpdateEvent;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.StartQueueEvent;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static com.zenith.util.Constants.*;

public class TabListDataHandler implements HandlerRegistry.AsyncIncomingHandler<ServerPlayerListDataPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerPlayerListDataPacket packet, @NonNull ClientSession session) {
        CACHE.getTabListCache().getTabList()
                .setHeader(packet.getHeader())
                .setFooter(packet.getFooter());
        parse2bQueueState(packet, session);

        MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(packet.getFooter());
        final String messageString = mcTextRoot.toRawString().replace("\n", "");
        /**
         * non prio:
         * "You can purchase priority queue status to join the server faster, visit shop.2b2t.org"
         *
         * prio:
         * "This account has priority status and will be placed in a shorter queue."
         */
        if (messageString.contains("priority")) {
            EVENT_BUS.dispatch(new PrioStatusUpdateEvent(!messageString.contains("shop.2b2t.org")));
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
                .filter(m -> m.contains("2b2t is full") || m.toLowerCase(Locale.ROOT).contains("pending"))
                .findAny();
        if (queueHeader.isPresent()) {
            if (!session.isInQueue()) {
                EVENT_BUS.dispatch(new StartQueueEvent());
            }
            session.setInQueue(true);
        } else if (session.isInQueue()) {
            session.setInQueue(false);
            session.setLastQueuePosition(Integer.MAX_VALUE);
            // temp adding this to debug why sometimes 2b causes many start queue events to occur
            CLIENT_LOG.debug("Queue complete event. Queue pos: {}, playerList packet header: {}", session.getLastQueuePosition(), packet.getHeader());
            EVENT_BUS.dispatch(new QueueCompleteEvent());
        } else if (!session.isOnline()) {
            session.setOnline(true);
            EVENT_BUS.dispatch(new PlayerOnlineEvent());
        }
    }

}
