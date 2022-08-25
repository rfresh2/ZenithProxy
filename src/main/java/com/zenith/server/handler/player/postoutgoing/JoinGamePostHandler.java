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

import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.zenith.cache.DataCache;
import com.zenith.server.ServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.CACHE;

/**
 * @author DaPorkchop_
 */
public class JoinGamePostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, ServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull ServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));

        //send cached data
        DataCache.sendCacheData(CACHE.getAllData(), session);

        // init any active spectators
        session.getProxy().getServerConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    session.send(new ServerSpawnPlayerPacket(
                            connection.getSpectatorEntityId(),
                            connection.getProfileCache().getProfile().getId(),
                            CACHE.getPlayerCache().getX(),
                            CACHE.getPlayerCache().getY(),
                            CACHE.getPlayerCache().getZ(),
                            CACHE.getPlayerCache().getYaw(),
                            CACHE.getPlayerCache().getPitch(),
                            CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));

                    session.send(new ServerSpawnMobPacket(
                            connection.getSpectatorEntityId(),
                            connection.getSpectatorCatUUID(),
                            MobType.OCELOT,
                            CACHE.getPlayerCache().getX(),
                            CACHE.getPlayerCache().getY(),
                            CACHE.getPlayerCache().getZ(),
                            CACHE.getPlayerCache().getYaw(),
                            CACHE.getPlayerCache().getPitch(),
                            CACHE.getPlayerCache().getYaw(),
                            0f,
                            0f,
                            0f,
                            connection.getSpectatorCatEntityMetadata(false)));
                });

        session.setLoggedIn(true);
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
