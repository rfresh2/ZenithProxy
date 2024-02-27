package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.object.ObjectData;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import com.github.steveice10.packetlib.packet.Packet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Data
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
    protected Map<AttributeType, Attribute> attributes = new ConcurrentHashMap<>();
    protected List<EntityMetadata> metadata = new ArrayList<>();
    protected IntArrayList passengerIds = new IntArrayList();
    protected boolean isInVehicle;
    protected int vehicleId;
    protected ObjectData objectData;

    public void addPackets(@NonNull Consumer<Packet> consumer)  {
        if (!this.attributes.isEmpty()) {
            consumer.accept(new ClientboundUpdateAttributesPacket(this.entityId, new ArrayList<>(attributes.values())));
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

    public void updateAttributes(@NonNull List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            this.attributes.put(attribute.getType(), attribute);
        }
    }

    public void mountVehicle(int vehicleId) {
        this.isInVehicle = true;
        this.vehicleId = vehicleId;
    }

    public void dismountVehicle() {
        this.isInVehicle = false;
        this.vehicleId = -1;
    }
}
