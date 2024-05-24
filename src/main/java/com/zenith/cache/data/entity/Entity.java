package com.zenith.cache.data.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ObjectData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Data
@Accessors(chain = true)
public abstract class Entity {
    protected EntityType entityType = EntityType.PLAYER;
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
    protected Int2ObjectMap<EntityMetadata<?, ?>> metadata = new Int2ObjectArrayMap<>();
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
            consumer.accept(new ClientboundSetEntityDataPacket(this.entityId, new ArrayList<>(metadata.values())));
        }
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
