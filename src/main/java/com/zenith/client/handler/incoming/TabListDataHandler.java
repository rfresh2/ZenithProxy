/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.zenith.event.proxy.PlayerOnlineEvent;
import com.zenith.event.proxy.PrioStatusUpdateEvent;
import com.zenith.event.proxy.QueueCompleteEvent;
import com.zenith.event.proxy.StartQueueEvent;
import lombok.NonNull;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
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

    private void parse2bQueueState(ServerPlayerListDataPacket packet, ClientSession session) {
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
            EVENT_BUS.dispatch(new QueueCompleteEvent());
        } else if (!session.isOnline()) {
            session.setOnline(true);
            EVENT_BUS.dispatch(new PlayerOnlineEvent());
        }
    }

}
