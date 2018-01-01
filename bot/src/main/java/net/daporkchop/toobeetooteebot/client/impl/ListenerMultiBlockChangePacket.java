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
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.util.ChunkPos;
import net.daporkchop.toobeetooteebot.util.Config;

public class ListenerMultiBlockChangePacket implements IPacketListener<ServerMultiBlockChangePacket> {
    @Override
    public void handlePacket(Session session, ServerMultiBlockChangePacket pck) {
        if (Config.doServer) {
            int chunkX = pck.getRecords()[0] //there HAS to be at least one element
                    .getPosition().getX() >> 4; //this cuts away the additional relative chunk coordinates
            int chunkZ = pck.getRecords()[0] //there HAS to be at least one element
                    .getPosition().getZ() >> 4; //this cuts away the additional relative chunk coordinates
            Column column = Caches.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
            if (column == null) {
                //unloaded or invalid chunk, ignore pls
                System.out.println("null chunk multi, this is probably a server bug");
                return;
            }
            for (BlockChangeRecord record : pck.getRecords()) {
                int relativeChunkX = Math.abs(Math.abs(record.getPosition().getX()) - (Math.abs(Math.abs(record.getPosition().getX() >> 4)) * 16));
                int relativeChunkZ = Math.abs(Math.abs(record.getPosition().getZ()) - (Math.abs(Math.abs(record.getPosition().getZ() >> 4)) * 16));
                int subchunkY = TooBeeTooTeeBot.ensureRange(record.getPosition().getY() >> 4, 0, 15);
                Chunk subChunk = column.getChunks()[subchunkY];
                int subchunkRelativeY = Math.abs(record.getPosition().getY() - 16 * subchunkY);
                try {
                    subChunk.getBlocks().set(relativeChunkX, TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), relativeChunkZ, record.getBlock());
                    column.getChunks()[subchunkY] = subChunk;
                } catch (IndexOutOfBoundsException e) {
                    System.out.println(relativeChunkX + " " + subchunkRelativeY + " " + relativeChunkZ + " " + (subchunkRelativeY << 8 | relativeChunkZ << 4 | relativeChunkX));
                }
            }
            Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
        }
    }
}
