/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.server.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.RefStrings;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author DaPorkchop_
 */
public class JoinGamePostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, PorkServerConnection> {
    @Override
    public void accept(ServerJoinGamePacket packet, PorkServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));

        //send cached data
        System.out.printf("Sending %d chunks...\n", CACHE.getChunks().size());
        CACHE.getChunks().values().stream().map(ServerChunkDataPacket::new).forEach(session::send);

        //this packet spawns the player
        session.send(new ServerPlayerPositionRotationPacket(
                CACHE.getPlayerPos().getX(),
                CACHE.getPlayerPos().getY(),
                CACHE.getPlayerPos().getZ(),
                0.0f,
                0.0f,
                ThreadLocalRandom.current().nextInt(16, 1024)
        ));
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
