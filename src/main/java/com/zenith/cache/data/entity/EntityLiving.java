package com.zenith.cache.data.entity;

import com.zenith.mc.entity.EntityData;
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

import static com.zenith.Globals.ENTITY_DATA;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class EntityLiving extends Entity {
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

}
