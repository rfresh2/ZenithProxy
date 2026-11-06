package com.zenith.cache.data.entity;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.mc.entity.EntityData;
import com.zenith.util.math.MathHelper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Equipment;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateMobEffectPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.ENTITY_DATA;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityLiving extends Entity {
    protected int lerpSteps;
    protected double lerpX;
    protected double lerpY;
    protected double lerpZ;
    protected double lerpYaw;
    protected double lerpPitch;
    protected double lerpHeadYaw;
    protected int lerpHeadSteps;
    @Nullable
    protected Float health;
    protected Map<Effect, PotionEffect> potionEffectMap = new EnumMap<>(Effect.class);
    protected Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    @Override
    public void addPackets(final @NonNull Consumer<Packet> consumer) {
        if (!potionEffectMap.isEmpty()) {
            this.getPotionEffectMap().forEach((effect, potionEffect) -> consumer.accept(new ClientboundUpdateMobEffectPacket(
                this.entityId,
                effect,
                potionEffect.getAmplifier(),
                potionEffect.getDuration(),
                potionEffect.isAmbient(),
                potionEffect.isShowParticles(),
                potionEffect.isShowIcon(),
                potionEffect.isBlend()
            )));
        }
        if (!isSelfPlayer() && !getEquipment().isEmpty()) {
            consumer.accept(new ClientboundSetEquipmentPacket(this.entityId, getEquipment().entrySet().stream()
                .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                .toList()));
        }
        super.addPackets(consumer);
    }

    private boolean isSelfPlayer() {
        return this instanceof EntityPlayer player && player.isSelfPlayer();
    }

    public boolean isAlive() {
        if (removed) return false;
        // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity
        EntityMetadata<?, ?> poseMetadata = getMetadata().get(6);
        if (poseMetadata != null && poseMetadata.getType() == MetadataTypes.POSE) {
            var pose = (Pose) poseMetadata.getValue();
            if (pose == Pose.DYING) return false;
        }
        return true;
    }

    public boolean isSleeping() {
        if (removed) return false;
        // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Living_Entity
        EntityMetadata<?, ?> bedLocationMetadata = getMetadata().get(14);
        if (bedLocationMetadata != null && bedLocationMetadata.getType() == MetadataTypes.OPTIONAL_POSITION) {
            var bedLocation = (Optional<Vector3i>) bedLocationMetadata.getValue();
            if (bedLocation.isPresent()) return true;
        }
        return false;
    }

    public boolean isSwimming() {
        if (removed) return false;
        Byte flagsByte = getMetadataValue(0, MetadataTypes.BYTE, Byte.class);
        if (flagsByte == null) return false;
        return (flagsByte & (1 << 4)) != 0;
    }

    public boolean isBaby() {
        if (removed) return false;
        var entityData = ENTITY_DATA.getEntityData(entityType);
        if (entityType == null) return false;
        int metadataIndex = -1;

        if (entityData.ageableMob()) {
            // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Ageable_Mob
            metadataIndex = 16;
        } else if (entityType == EntityType.HOGLIN) {
            // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Hoglin
            metadataIndex = 16;
        } else if (entityType == EntityType.ZOMBIE) {
            // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Zombie
            metadataIndex = 16;
        } else if (entityType == EntityType.PIGLIN) {
            // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Piglin
            metadataIndex = 17;
        }
        if (metadataIndex == -1) return false;
        var metadataValue = getMetadataValue(metadataIndex, MetadataTypes.BOOLEAN, Boolean.class);
        if (metadataValue == null) return false;
        return metadataValue;
    }

    @Override
    public Vector2d dimensions() {
        EntityData entityData = ENTITY_DATA.getEntityData(entityType);
        if (entityData != null) {
            var dimensions = super.dimensions();
            if (isBaby()) {
                dimensions = dimensions.mul(0.5);
            }
            var scaleAttribute = attributes.get(AttributeType.Builtin.SCALE);
            if (scaleAttribute != null) {
                dimensions = dimensions.mul(scaleAttribute.getValue());
            }
            return dimensions;
        }
        return Vector2d.ZERO;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = yaw;
        this.lerpPitch = pitch;
        this.lerpSteps = steps;
    }

    @Override
    public void lerpHeadTo(float yaw, int steps) {
        this.lerpHeadYaw = yaw;
        this.lerpHeadSteps = steps;
    }

    @Override
    public void cancelLerp() {
        this.lerpSteps = 0;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    public float lerpTargetPitch() {
        return this.lerpSteps > 0 ? (float) this.lerpPitch : this.getPitch();
    }

    public float lerpTargetYaw() {
        return this.lerpSteps > 0 ? (float) this.lerpYaw : this.getYaw();
    }

    public float lerpTargetHeadYaw() {
        return this.lerpHeadSteps > 0 ? (float) this.lerpHeadYaw : this.getHeadYaw();
    }

    @Override
    public void tick() {
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYaw, this.lerpPitch);
            if (passengerIds.contains(CACHE.getPlayerCache().getThePlayer().getEntityId())) {
                SpectatorSync.syncPlayerPositionWithSpectators();
            }
            this.lerpSteps--;
        }
        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpHeadYaw);
            if (passengerIds.contains(CACHE.getPlayerCache().getThePlayer().getEntityId())) {
                SpectatorSync.syncPlayerPositionWithSpectators();
            }
            this.lerpHeadSteps--;
        }
    }

    protected void lerpPositionAndRotationStep(int steps, double targetX, double targetY, double targetZ, double targetYaw, double targetPitch) {
        var delta = 1.0 / steps;
        setX(MathHelper.lerp(delta, this.getX(), targetX));
        setY(MathHelper.lerp(delta, this.getY(), targetY));
        setZ(MathHelper.lerp(delta, this.getZ(), targetZ));
        setYaw((float)MathHelper.rotLerp(delta, this.getYaw(), targetYaw));
        setPitch((float)MathHelper.lerp(delta, this.getPitch(), targetPitch));
    }

    protected void lerpHeadRotationStep(int lerpHeadSteps, double lerpHeadYaw) {
        this.headYaw = (float) MathHelper.rotLerp(1.0 / lerpHeadSteps, this.headYaw, lerpHeadYaw);
    }
}
