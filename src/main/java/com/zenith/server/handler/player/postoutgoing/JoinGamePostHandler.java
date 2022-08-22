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

package com.zenith.server.handler.player.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.Wait;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.concurrent.ForkJoinPool;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class JoinGamePostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, PorkServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull PorkServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));

        //send cached data
        CACHE.getAllData().forEach(data -> {
            if (CONFIG.debug.server.cache.sendingmessages) {
                String msg = data.getSendingMessage();
                if (msg == null)    {
                    SERVER_LOG.debug("Sending data for %s", data.getClass().getCanonicalName());
                } else {
                    SERVER_LOG.debug(msg);
                }
            }
            data.getPackets(p -> {
                if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                    return;
                }
                session.send(p);
            });
            ForkJoinPool.commonPool().submit(() -> {
                // client needs to receive chunks first.
                // this wait is kinda arbitrary and may be too short or long for some clients
                // likely dependent on client net speed
                // we don't have a good hook into when the client is done receiving chunks though.
                // waiting too long will appear as though chunks are visibly updating for client during play
                Wait.waitALittle(1);
                data.getPackets(p -> {
                    if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                        session.send(p);
                    }
                });
            });
        });

        session.setLoggedIn(true);
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
