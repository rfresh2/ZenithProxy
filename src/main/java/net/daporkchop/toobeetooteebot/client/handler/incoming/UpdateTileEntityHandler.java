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

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class UpdateTileEntityHandler implements HandlerRegistry.IncomingHandler<ServerUpdateTileEntityPacket, PorkClientSession> {
    protected static final long COLUMN_TILEENTITIES_OFFSET = PUnsafe.pork_getOffset(Column.class, "tileEntities");

    @Override
    public boolean apply(@NonNull ServerUpdateTileEntityPacket packet, @NonNull PorkClientSession session) {
        Column column = CACHE.getChunkCache().get(packet.getPosition().getX() >> 4, packet.getPosition().getZ() >> 4);
        CompoundTag[] oldArray = column.getTileEntities();
        int index = -1;
        for (int i = oldArray.length - 1; i >= 0; i--)  {
            if (oldArray[i].<IntTag>get("x").getValue() == packet.getPosition().getX()
                    && oldArray[i].<IntTag>get("y").getValue() == packet.getPosition().getY()
                    && oldArray[i].<IntTag>get("z").getValue() == packet.getPosition().getZ())  {
                index = i;
                break;
            }
        }
        CompoundTag[] newArray;
        if (packet.getNBT() == null)    {
            if (index == -1)    {
                newArray = oldArray;
            } else {
                newArray = new CompoundTag[oldArray.length - 1];
                System.arraycopy(oldArray, 0, newArray, 0, index - 1);
                System.arraycopy(oldArray, index + 1, newArray, index, oldArray.length - index - 1);
            }
        } else {
            if (index == -1)    {
                newArray = new CompoundTag[oldArray.length + 1];
                System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
                newArray[oldArray.length] = packet.getNBT();
            } else {
                newArray = oldArray;
                newArray[index] = packet.getNBT();
            }
            packet.getNBT().put(new IntTag("x", packet.getPosition().getX()));
            packet.getNBT().put(new IntTag("y", packet.getPosition().getY()));
            packet.getNBT().put(new IntTag("z", packet.getPosition().getZ()));
        }
        PUnsafe.putObject(column, COLUMN_TILEENTITIES_OFFSET, newArray);
        return true;
    }

    @Override
    public Class<ServerUpdateTileEntityPacket> getPacketClass() {
        return ServerUpdateTileEntityPacket.class;
    }
}
