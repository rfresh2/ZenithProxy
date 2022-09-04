package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
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
        if (!this.metadata.isEmpty()) {
            consumer.accept(new ServerEntityMetadataPacket(this.entityId, this.getEntityMetadataAsArray()));
        }
    }

    public int[] getPassengerIdsAsArray()  {
        int[] arr = new int[this.passengerIds.size()];
        for (int i = 0; i < arr.length; i++)    {
            arr[i] = this.passengerIds.get(i);
        }
        return arr;
    }

    public EntityMetadata[] getEntityMetadataAsArray() {
        EntityMetadata[] arr = new EntityMetadata[this.metadata.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = this.metadata.get(i);
        }
        return arr;
    }
}
