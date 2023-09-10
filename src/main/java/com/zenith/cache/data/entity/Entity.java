package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.object.ObjectData;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import com.github.steveice10.packetlib.packet.Packet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
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
    protected IntArrayList passengerIds = new IntArrayList();
    protected ObjectData objectData;

    public void addPackets(@NonNull Consumer<Packet> consumer)  {
        if (!this.properties.isEmpty()) {
            consumer.accept(new ClientboundUpdateAttributesPacket(this.entityId, this.properties));
        }
        if (!this.passengerIds.isEmpty()) {
            consumer.accept(new ClientboundSetPassengersPacket(this.entityId, passengerIds.toIntArray()));
        }
        if (!this.metadata.isEmpty()) {
            consumer.accept(new ClientboundSetEntityDataPacket(this.entityId, metadata.toArray(new EntityMetadata[0])));
        }
    }

    public EntityMetadata[] getEntityMetadataAsArray() {
        return metadata.toArray(new EntityMetadata[0]);
    }
}
