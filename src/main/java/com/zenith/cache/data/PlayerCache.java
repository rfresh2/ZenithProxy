package com.zenith.cache.data;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.GlobalPos;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.CreativeGrabAction;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateTagsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.cache.data.entity.EntityPlayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;


@Getter
@Setter
@Accessors(chain = true)
public class PlayerCache implements CachedData {
    protected boolean hardcore;
    protected boolean reducedDebugInfo;
    protected int maxPlayers;
    protected boolean enableRespawnScreen;
    protected GlobalPos lastDeathPos;
    protected int portalCooldown;
    protected GameMode gameMode;
    protected int heldItemSlot = 0;

    protected EntityPlayer thePlayer;

    protected final ItemStack[] inventory = new ItemStack[46];

    protected final EntityCache entityCache;
    protected String[] enabledFeatures = new String[]{"minecraft:vanilla"};
    protected Difficulty difficulty = Difficulty.NORMAL;
    protected boolean isDifficultyLocked;
    protected boolean invincible;
    protected boolean canFly;
    protected boolean flying;
    protected boolean creative;
    protected float flySpeed;
    protected float walkSpeed;
    protected boolean isSneaking = false;
    protected boolean isSprinting = false;
    protected Map<String, Map<String, int[]>> tags = new HashMap<>();
    protected EntityEvent opLevel = EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0;

    public PlayerCache(final EntityCache entityCache) {
        this.entityCache = entityCache;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateEnabledFeaturesPacket(this.enabledFeatures));
        // todo: may need to move this out so spectators don't get sent wrong abilities
        consumer.accept(new ClientboundPlayerAbilitiesPacket(this.invincible, this.canFly, this.flying, this.creative, this.flySpeed, this.walkSpeed));
        consumer.accept(new ClientboundChangeDifficultyPacket(this.difficulty, this.isDifficultyLocked));
        consumer.accept(new ClientboundUpdateTagsPacket(this.tags));
        consumer.accept(new ClientboundGameEventPacket(GameEvent.CHANGE_GAMEMODE, this.gameMode));
        consumer.accept(new ClientboundEntityEventPacket(this.thePlayer.getEntityId(), this.opLevel));
        consumer.accept(new ClientboundContainerSetContentPacket(0,
                                                                 0, // todo: verify if this is correct
                                                                 this.inventory.clone(),
                                                                 new ItemStack(0, 0)));
        consumer.accept(new ClientboundPlayerPositionPacket(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), ThreadLocalRandom.current().nextInt(16, 1024)));
        consumer.accept(new ClientboundSetCarriedItemPacket(heldItemSlot));
    }

    @Override
    public void reset(boolean full) {
        if (full)   {
            this.thePlayer = (EntityPlayer) new EntityPlayer(true).setEntityId(-1);
            this.hardcore = this.reducedDebugInfo = false;
            this.maxPlayers = -1;
            Arrays.fill(this.inventory, null);
            this.heldItemSlot = 0;
            this.enabledFeatures = new String[0];
        }
        this.gameMode = null;
        this.thePlayer.setHealth(20.0f);
        this.thePlayer.setFood(20);
        this.thePlayer.setSaturation(5);
        this.thePlayer.getPotionEffectMap().clear();
        this.isSneaking = this.isSprinting = false;
    }

    @Override
    public String getSendingMessage() {
        return String.format(
                "Sending player position: (x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f)",
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getYaw(),
                this.getPitch()
        );
    }

    public static void sync() {
        if (nonNull(Proxy.getInstance().getClient())) {
            // todo: verify this still works
            try {
                // intentionally sends an invalid inventory packet to issue a ServerWindowItems which corrects all inventory slot contents
                // pretty sure it requires a Notchian client to be connected to send the confirmTransaction stuff, can be implemented later if nesscesary
                Proxy.getInstance().getClient().sendAsync(new ServerboundContainerClickPacket(0,
                                                                                         -1337,
                                                                                         0,
                                                                                         ContainerActionType.CREATIVE_GRAB_MAX_STACK,
                                                                                         CreativeGrabAction.GRAB,
                                                                                         new ItemStack(1, 1),
                                                                                         Int2ObjectMaps.emptyMap()), SCHEDULED_EXECUTOR_SERVICE);
                double x = CACHE.getPlayerCache().getX();
                double y = CACHE.getPlayerCache().getY() + 1000d;
                double z = CACHE.getPlayerCache().getZ();
                // one of 2b2t's plugins requires this (as of 2022)
//                Proxy.getInstance().getClient().sendDirect(new ServerboundMovePlayerPosPacket(true, x, y, z));
            } catch (final Exception e) {
                CLIENT_LOG.warn("Failed Player Sync", e);
            }
        }
    }

    public void setInventory(ItemStack[] newInventory) {
        System.arraycopy(newInventory, 0, this.inventory, 0, Math.min(this.inventory.length, newInventory.length));
        final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HELMET, this.inventory[5]);
        equipment.put(EquipmentSlot.CHESTPLATE, this.inventory[6]);
        equipment.put(EquipmentSlot.LEGGINGS, this.inventory[7]);
        equipment.put(EquipmentSlot.BOOTS, this.inventory[8]);
        equipment.put(EquipmentSlot.OFF_HAND, this.inventory[45]);
        equipment.put(EquipmentSlot.MAIN_HAND, this.inventory[heldItemSlot + 36]);
        this.getThePlayer().setEquipment(equipment);
    }

    public void setInventorySlot(ItemStack newItemStack, int slot) {
        this.inventory[slot] = newItemStack;
        if (slot >= 5 && slot <= 8 || slot == 45 || slot == heldItemSlot) {
            switch (slot) {
                case 5 -> this.getThePlayer().getEquipment().put(EquipmentSlot.HELMET, newItemStack);
                case 6 -> this.getThePlayer().getEquipment().put(EquipmentSlot.CHESTPLATE, newItemStack);
                case 7 -> this.getThePlayer().getEquipment().put(EquipmentSlot.LEGGINGS, newItemStack);
                case 8 -> this.getThePlayer().getEquipment().put(EquipmentSlot.BOOTS, newItemStack);
                case 45 -> this.getThePlayer().getEquipment().put(EquipmentSlot.OFF_HAND, newItemStack);
            }
        }
        if (slot == heldItemSlot + 36) {
            getThePlayer().getEquipment().put(EquipmentSlot.MAIN_HAND, newItemStack);
        }
    }

    public PlayerCache setHeldItemSlot(final int slot) {
        this.heldItemSlot = slot;
        getThePlayer().getEquipment().put(EquipmentSlot.MAIN_HAND, getInventory()[slot + 36]);
        return this;
    }

    public double getX() {
        return this.thePlayer.getX();
    }

    public PlayerCache setX(double x) {
        this.thePlayer.setX(x);
        return this;
    }

    public double getY()    {
        return this.thePlayer.getY();
    }

    public PlayerCache setY(double y)    {
        this.thePlayer.setY(y);
        return this;
    }

    public double getZ()    {
        return this.thePlayer.getZ();
    }

    public PlayerCache setZ(double z)    {
        this.thePlayer.setZ(z);
        return this;
    }

    public float getYaw()    {
        return this.thePlayer.getYaw();
    }

    public PlayerCache setYaw(float yaw)    {
        this.thePlayer.setYaw(yaw);
        return this;
    }

    public float getPitch()    {
        return this.thePlayer.getPitch();
    }

    public PlayerCache setPitch(float pitch)    {
        this.thePlayer.setPitch(pitch);
        return this;
    }

    public int getEntityId()    {
        return this.thePlayer.getEntityId();
    }

    public PlayerCache setEntityId(int id)  {
        if (this.thePlayer.getEntityId() != -1) {
            this.entityCache.remove(this.thePlayer.getEntityId());
        }
        this.thePlayer.setEntityId(id);
        this.entityCache.add(this.thePlayer);
        return this;
    }

    public PlayerCache setUuid(UUID uuid) {
        this.thePlayer.setUuid(uuid);
        return this;
    }
}
