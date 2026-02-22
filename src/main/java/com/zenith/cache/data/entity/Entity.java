package com.zenith.cache.data.entity;

import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.mc.entity.EntityData;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ObjectData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.zenith.Globals.ENTITY_DATA;


@Data
@Accessors(chain = true)
public abstract class Entity {
    @ToString.Include protected EntityType entityType = EntityType.PLAYER;
    protected double x;
    protected double y;
    protected double z;
    protected float yaw;
    protected float pitch;
    protected float headYaw;
    @ToString.Include protected int entityId;
    protected UUID uuid;
    protected double velX;
    protected double velY;
    protected double velZ;
    protected int leashedId;
    protected boolean isLeashed;
    protected Map<AttributeType, Attribute> attributes = new ConcurrentHashMap<>();
    protected Int2ObjectMap<EntityMetadata<?, ?>> metadata = new Int2ObjectOpenHashMap<>();
    protected IntArrayList passengerIds = new IntArrayList();
    protected boolean isInVehicle;
    protected int vehicleId;
    protected ObjectData objectData;
    protected boolean removed = false;
    protected int tickCount = 0;

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

    public EntityData getEntityData() {
        return ENTITY_DATA.getEntityData(entityType);
    }

    public BlockPos blockPos() {
        return new BlockPos(MathHelper.floorI(this.x), MathHelper.floorI(this.y), MathHelper.floorI(this.z));
    }

    public Vector3d position() {
        return Vector3d.from(this.x, this.y, this.z);
    }

    public double distanceSqTo(Entity entity) {
        return this.position().distanceSquared(entity.position());
    }

    public <T> @Nullable T getMetadataValue(int index, MetadataType<T> metadataType, Class<T> valueClass) {
        var metadata = this.metadata.get(index);
        if (metadata == null) return null;
        if (metadata.getType() == metadataType) {
            var metadataValue = metadata.getValue();
            if (metadataValue == null) {
                return null;
            }
            if (valueClass.isInstance(metadataValue)) {
                return valueClass.cast(metadata.getValue());
            }
        }
        return null;
    }

    public Pose getPose() {
        Pose pose = getMetadataValue(6, MetadataTypes.POSE, Pose.class);
        return pose != null ? pose : Pose.STANDING;
    }

    public Vector2d dimensions() {
        EntityData entityData = ENTITY_DATA.getEntityData(entityType);
        if (entityData != null) {
            return Vector2d.from(entityData.width(), entityData.height());
        }
        return Vector2d.ZERO;
    }

    public LocalizedCollisionBox collisionBox() {
        var dimensions = dimensions();
        double width = dimensions.getX();
        double height = dimensions.getY();
        double x = getX();
        double y = getY();
        double z = getZ();
        double minX = x - width / 2;
        double maxX = x + width / 2;
        double minY = y;
        double maxY = y + height;
        double minZ = z - width / 2;
        double maxZ = z + width / 2;
        return new LocalizedCollisionBox(minX, maxX, minY, maxY, minZ, maxZ, x, y, z);
    }

}
