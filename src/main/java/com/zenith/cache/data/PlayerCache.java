package com.zenith.cache.data;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.cache.data.entity.EntityPlayer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.zenith.util.Constants.CACHE;


@Getter
@Setter
@Accessors(chain = true)
public class PlayerCache implements CachedData {
    protected boolean hardcore;
    protected boolean reducedDebugInfo;
    protected int maxPlayers;
    protected int dimension;
    protected GameMode gameMode;
    protected WorldType worldType;
    protected Difficulty difficulty;

    protected EntityPlayer thePlayer;

    protected final ItemStack[] inventory = new ItemStack[46];

    protected final EntityCache entityCache;

    public PlayerCache(final EntityCache entityCache) {
        this.entityCache = entityCache;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ServerWindowItemsPacket(0, this.inventory.clone()));
        consumer.accept(new ServerPlayerPositionRotationPacket(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), ThreadLocalRandom.current().nextInt(16, 1024)));
    }

    @Override
    public void reset(boolean full) {
        if (full)   {
            this.thePlayer = (EntityPlayer) new EntityPlayer(true).setEntityId(-1);
            this.hardcore = this.reducedDebugInfo = false;
            this.maxPlayers = -1;
            Arrays.fill(this.inventory, null);
        }
        this.dimension = Integer.MAX_VALUE;
        this.gameMode = null;
        this.worldType = null;
        this.difficulty = null;
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
        // intentionally sends an invalid inventory packet to issue a ServerWindowItems which corrects all inventory slot contents
        // pretty sure it requires a Notchian client to be connected to send the confirmTransaction stuff, can be implemented later if nesscesary
        Proxy.getInstance().getClient().send(new ClientWindowActionPacket(0, -1337, 0, new ItemStack(1, 1), WindowAction.CREATIVE_GRAB_MAX_STACK, ClickItemParam.LEFT_CLICK));
        double x = CACHE.getPlayerCache().getX();
        double y = CACHE.getPlayerCache().getY() + 1000d;
        double z = CACHE.getPlayerCache().getZ();
        // one of 2b2t's plugins requires this (as of 2022)
        Proxy.getInstance().getClient().sendDirect(new ClientPlayerPositionPacket(true, x, y, z));
    }

    public void setInventory(ItemStack[] newInventory) {
        System.arraycopy(newInventory, 0, this.inventory, 0, Math.min(this.inventory.length, newInventory.length));
        final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HELMET, this.inventory[5]);
        equipment.put(EquipmentSlot.CHESTPLATE, this.inventory[6]);
        equipment.put(EquipmentSlot.LEGGINGS, this.inventory[7]);
        equipment.put(EquipmentSlot.BOOTS, this.inventory[8]);
        equipment.put(EquipmentSlot.OFF_HAND, this.inventory[45]);
        this.getThePlayer().setEquipment(equipment);
    }

    public void setInventorySlot(ItemStack newItemStack, int slot) {
        this.inventory[slot] = newItemStack;
        if (slot >= 5 && slot <= 8 || slot == 45) {
            switch (slot) {
                case 5:
                    this.getThePlayer().getEquipment().put(EquipmentSlot.HELMET, newItemStack);
                    break;
                case 6:
                    this.getThePlayer().getEquipment().put(EquipmentSlot.CHESTPLATE, newItemStack);
                    break;
                case 7:
                    this.getThePlayer().getEquipment().put(EquipmentSlot.LEGGINGS, newItemStack);
                    break;
                case 8:
                    this.getThePlayer().getEquipment().put(EquipmentSlot.BOOTS, newItemStack);
                    break;
                case 45:
                    this.getThePlayer().getEquipment().put(EquipmentSlot.OFF_HAND, newItemStack);
                    break;
            }
        }
    }

    public double getX()    {
        return this.thePlayer.getX();
    }

    public PlayerCache setX(double x)    {
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
}
