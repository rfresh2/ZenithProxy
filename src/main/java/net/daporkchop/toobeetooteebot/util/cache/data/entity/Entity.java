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

package net.daporkchop.toobeetooteebot.util.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class Entity {
    protected double x;
    protected double y;
    protected double z;
    protected float yaw;
    protected float pitch;
    protected float headYaw;
    protected int entityId;
    protected UUID uuid;
    protected double velX;
    protected double velY;
    protected double velZ;
    protected int leashedId;
    protected boolean isLeashed;
    protected List<Attribute> properties = new ArrayList<>();
    protected List<EntityMetadata> metadata = new ArrayList<>();
    protected List<Integer> passengerIds = new ArrayList<>(); //TODO: primitive list

    public void addPackets(@NonNull Consumer<Packet> consumer)  {
        if (!this.properties.isEmpty()) {
            consumer.accept(new ServerEntityPropertiesPacket(this.entityId, this.properties));
        }
        if (!this.passengerIds.isEmpty())   {
            consumer.accept(new ServerEntitySetPassengersPacket(this.entityId, this.getPassengerIdsAsArray()));
        }
    }

    public int[] getPassengerIdsAsArray()  {
        int[] arr = new int[this.passengerIds.size()];
        for (int i = 0; i < arr.length; i++)    {
            arr[i] = this.passengerIds.get(i);
        }
        return arr;
    }
}
