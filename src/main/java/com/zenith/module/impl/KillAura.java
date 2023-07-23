package com.zenith.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.data.game.window.MoveToHotbarParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.google.common.collect.Sets;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityMob;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import net.daporkchop.lib.math.vector.Vec3d;

import javax.annotation.Nullable;
import java.util.Set;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class KillAura extends Module {

    private static final Set<MobType> friendlyMobTypes = Sets.newHashSet(
            MobType.SKELETON_HORSE, MobType.ZOMBIE_HORSE, MobType.ARMOR_STAND, MobType.DONKEY, MobType.MULE, MobType.ZOMBIE_PIGMAN,
            MobType.BAT, MobType.PIG, MobType.SHEEP, MobType.COW, MobType.CHICKEN, MobType.SQUID, MobType.WOLF,
            MobType.MOOSHROOM, MobType.OCELOT, MobType.IRON_GOLEM, MobType.HORSE, MobType.RABBIT, MobType.POLAR_BEAR,
            MobType.LLAMA, MobType.PARROT, MobType.VILLAGER
    );
    private int delay = 0;
    private boolean isAttacking = false;
    private EquipmentSlot weaponSlot = EquipmentSlot.MAIN_HAND;
    private int actionId = 0; // todo: might need to track this in cache. this will be inaccurate incrementing in many cases
    private boolean swapping = false;

    public boolean active() {
        return CONFIG.client.extra.killAura.enabled && isAttacking;
    }

    @Subscribe
    public void handleClientTick(final ClientTickEvent event) {
        if (CONFIG.client.extra.killAura.enabled
                && CACHE.getPlayerCache().getThePlayer().getHealth() > 0
                && !Proxy.getInstance().isInQueue()
                && isNull(Proxy.getInstance().getCurrentPlayer().get())
                && !MODULE_MANAGER.getModule(AutoEat.class).map(AutoEat::isEating).orElse(false)
                && !MODULE_MANAGER.getModule(AutoTotem.class).map(AutoTotem::isActive).orElse(false)) {
            if (delay > 0) {
                delay--;
                return;
            }
            if (swapping) {
                PlayerCache.sync();
                delay = 5;
                swapping = false;
                return;
            }
            // find non-friended players or hostile mobs within 3.5 blocks
            final Entity target = findTarget();
            // rotate to target
            if (target != null && PATHING.isOnGround()) {
                if (switchToWeapon()) {
                    isAttacking = true;
                    if (rotateTo(target)) {
                        // attack
                        attack(target);
                        delay = 5;
                    }
                }
            } else {
                isAttacking = false;
            }
        }
    }

    @Nullable
    private Entity findTarget() {
        for (Entity entity : CACHE.getEntityCache().getEntities().values()) {
            if (!(entity instanceof EntityPlayer || entity instanceof EntityMob)) continue;
            if (entity instanceof EntityPlayer) {
                if (!CONFIG.client.extra.killAura.targetPlayers) continue;
                if (((EntityPlayer) entity).isSelfPlayer()) continue;
                if (WHITELIST_MANAGER.isUUIDFriendWhitelisted(entity.getUuid()) || WHITELIST_MANAGER.isUUIDWhitelisted(entity.getUuid()) || WHITELIST_MANAGER.isUUIDSpectatorWhitelisted(entity.getUuid())) continue;
            } else {
                if (!CONFIG.client.extra.killAura.targetMobs) continue;
                if (CONFIG.client.extra.killAura.avoidFriendlyMobs && friendlyMobTypes.contains(((EntityMob) entity).getMobType())) continue;
            }
            if (distanceToSelf(entity) > 4.5) continue;
            return entity;
        }
        return null;
    }

    @Override
    public void clientTickStopping() {
        delay = 0;
        isAttacking = false;
    }

    private void attack(final Entity entity) {
        sendClientPacketAsync(new ClientPlayerInteractEntityPacket(entity.getEntityId(), InteractAction.ATTACK));
        sendClientPacketAsync(new ClientPlayerSwingArmPacket(weaponSlot == EquipmentSlot.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND));
    }

    private boolean rotateTo(Entity entity) {
        final Vec3d playerVec = Vec3d.of(CACHE.getPlayerCache().getThePlayer().getX(), CACHE.getPlayerCache().getY() + 1, CACHE.getPlayerCache().getZ());
        final Vec3d entityVec = Vec3d.of(entity.getX(), entity.getY() + 0.2, entity.getZ());
        final Vec3d targetVec = entityVec.sub(playerVec);
        final double xz = Math.hypot(targetVec.x(), targetVec.z());
        final double yaw = normalizeAngle(Math.toDegrees(Math.atan2(targetVec.z(), targetVec.x())) - 90.0);
        final double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(targetVec.y(), xz)));
        final double currentYaw = CACHE.getPlayerCache().getYaw();
        final double currentPitch = CACHE.getPlayerCache().getPitch();
        if (!((currentYaw + 0.01 > yaw && currentYaw - 0.01 < yaw) && (currentPitch + 0.01 > pitch && currentPitch - 0.01 < pitch))) {
            sendClientPacketAsync(new ClientPlayerRotationPacket(false, (float) yaw, (float) pitch));
            return false;
        }
        return true;
    }

    private double normalizeAngle(double angleIn) {
        double angle = angleIn;
        angle %= 360.0;
        if (angle >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    private double distanceToSelf(final Entity entity) {
        return Math.sqrt(
                Math.pow(CACHE.getPlayerCache().getX() - entity.getX(), 2)
                        + Math.pow(CACHE.getPlayerCache().getY() - entity.getY(), 2)
                        + Math.pow(CACHE.getPlayerCache().getZ() - entity.getZ(), 2));
    }

    public boolean switchToWeapon() {
        if (!CONFIG.client.extra.killAura.switchWeapon) {
            return true;
        }

        // check if offhand has weapon
        final ItemStack offhandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.OFF_HAND);
        if (nonNull(offhandStack)) {
            if (isWeapon(offhandStack.getId())) {
                weaponSlot = EquipmentSlot.OFF_HAND;
                return true;
            }
        }
        // check mainhand
        final ItemStack mainHandStack = CACHE.getPlayerCache().getThePlayer().getEquipment().get(EquipmentSlot.MAIN_HAND);
        if (nonNull(mainHandStack)) {
            if (isWeapon(mainHandStack.getId())) {
                weaponSlot = EquipmentSlot.MAIN_HAND;
                return true;
            }
        }

        // find next weapon and switch it into our hotbar slot
        final ItemStack[] inventory = CACHE.getPlayerCache().getInventory();
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory[i];
            if (nonNull(stack) && isWeapon(stack.getId())) {
                sendClientPacketAsync(new ClientWindowActionPacket(0, actionId++, i, new ItemStack(0, 0), WindowAction.MOVE_TO_HOTBAR_SLOT, MoveToHotbarParam.SLOT_2));
                if (CACHE.getPlayerCache().getHeldItemSlot() != 1) {
                    sendClientPacketAsync(new ClientPlayerChangeHeldItemPacket(1));
                }
                delay = 5;
                swapping = true;
                weaponSlot = EquipmentSlot.MAIN_HAND;
                return false;
            }
        }
        // no weapon, let's just punch em
        weaponSlot = EquipmentSlot.MAIN_HAND;
        return true;
    }

    private boolean isWeapon(int id) {
        return
                id == 276 // diamond sword
                        || id == 283 // gold sword
                        || id == 267 // iron sword
                        || id == 272 // stone sword
                        || id == 268 // wooden sword
                        || id == 271 // wooden axe
                        || id == 275 // stone axe
                        || id == 258 // iron axe
                        || id == 286 // gold axe
                        || id == 279; // diamond axe
    }

}
