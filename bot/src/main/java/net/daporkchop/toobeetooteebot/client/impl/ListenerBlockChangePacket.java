/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot.client.impl;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.util.ChunkPos;
import net.daporkchop.toobeetooteebot.util.Config;

public class ListenerBlockChangePacket implements IPacketListener<ServerBlockChangePacket> {
    @Override
    public void handlePacket(Session session, ServerBlockChangePacket pck) {
        if (Config.doServer) {
            int chunkX = pck.getRecord().getPosition().getX() >> 4;
            int chunkZ = pck.getRecord().getPosition().getZ() >> 4;
            int subchunkY = TooBeeTooTeeBot.ensureRange(pck.getRecord().getPosition().getY() >> 4, 0, 15);
            Column column = Caches.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
            if (column == null) {
                //unloaded or invalid chunk, ignore pls
                System.out.println("null chunk, this is probably a server bug");
                return;
            }
            Chunk subChunk = column.getChunks()[subchunkY];
            int subchunkRelativeY = Math.abs(pck.getRecord().getPosition().getY() - 16 * subchunkY);
            try {
                subChunk.getBlocks().set(Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16)), TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16)), pck.getRecord().getBlock());
                column.getChunks()[subchunkY] = subChunk;
                Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
            } catch (IndexOutOfBoundsException e) {
                System.out.println((Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + " " + subchunkRelativeY + " " + (Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16))) + " " + (subchunkRelativeY << 8 | chunkZ << 4 | chunkX));
            }
            Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
            //System.out.println("chunk " + chunkX + ":" + subchunkY + ":" + chunkZ + " relative pos " + (Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + ":" + TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15) + "(" + subchunkRelativeY + "):" + (Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(chunkZ) * 16)) + " original position " + pck.getRecord().getPosition().toString());
        }
    }
}
