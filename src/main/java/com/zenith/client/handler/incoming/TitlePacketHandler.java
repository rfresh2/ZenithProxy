package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.TitleAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.zenith.client.ClientSession;
import com.zenith.event.proxy.QueuePositionUpdateEvent;
import com.zenith.feature.handler.HandlerRegistry;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.util.Optional;

import static com.zenith.Shared.CLIENT_LOG;
import static com.zenith.Shared.EVENT_BUS;

public class TitlePacketHandler implements HandlerRegistry.AsyncIncomingHandler<ServerTitlePacket, ClientSession> {
    @Override
    public boolean applyAsync(ServerTitlePacket packet, ClientSession session) {
        parse2bQueuePos(packet, session);
        return true;
    }

    @Override
    public Class<ServerTitlePacket> getPacketClass() {
        return ServerTitlePacket.class;
    }

    private void parse2bQueuePos(ServerTitlePacket serverTitlePacket, final ClientSession session) {
        try {
            Optional<Integer> position = Optional.of(serverTitlePacket)
                    .filter(packet -> packet.getAction().equals(TitleAction.SUBTITLE))
                    .map(ServerTitlePacket::getSubtitle)
                    .map(title -> AutoMCFormatParser.DEFAULT.parse(title).toRawString())
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
                    EVENT_BUS.dispatch(new QueuePositionUpdateEvent(position.get()));
                }
                session.setLastQueuePosition(position.get());
            }
        } catch (final Exception e) {
            CLIENT_LOG.warn("Error parsing queue position from title packet", e);
        }
    }
}
