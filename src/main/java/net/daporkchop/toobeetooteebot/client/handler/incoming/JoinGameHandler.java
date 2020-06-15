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

package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.Arrays;
import java.util.stream.Collectors;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class JoinGameHandler implements HandlerRegistry.IncomingHandler<ServerJoinGamePacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerJoinGamePacket packet, @NonNull PorkClientSession session) {
        CACHE.getPlayerCache()
                .setEntityId(packet.getEntityId())
                .setDimension(packet.getDimension())
                .setWorldType(packet.getWorldType())
                .setGameMode(packet.getGameMode())
                .setDifficulty(packet.getDifficulty())
                .setHardcore(packet.getHardcore())
                .setMaxPlayers(packet.getMaxPlayers())
                .setReducedDebugInfo(packet.getReducedDebugInfo());

        session.send(new ClientSettingsPacket(
                "en_US",
                8,
                ChatVisibility.FULL,
                true,
                SkinPart.values(),
                Hand.MAIN_HAND
        ));
        return false;
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
