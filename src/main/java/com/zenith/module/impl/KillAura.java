package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.MoveToHotbarAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.google.common.collect.Sets;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.Subscription;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import com.zenith.util.Maps;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class KillAura extends Module {

    private static final Set<EntityType> hostileEntities = Sets.newHashSet(
        EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN,
        EntityType.ENDER_DRAGON, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.GHAST, EntityType.GUARDIAN,
        EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.FIREBALL, EntityType.MAGMA_CUBE,
        EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER,
        EntityType.SHULKER, EntityType.SHULKER_BULLET, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME,
        EntityType.SMALL_FIREBALL, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR,
        EntityType.WARDEN, EntityType.WITCH, EntityType.WITHER, EntityType.ZOGLIN, EntityType.ZOMBIE,
        EntityType.ZOMBIE_VILLAGER
    );
    private int delay = 0;
    private boolean isAttacking = false;
    private EquipmentSlot weaponSlot = EquipmentSlot.MAIN_HAND;
    private int actionId = 0; // todo: might need to track this in cache. this will be inaccurate incrementing in many cases
    private boolean swapping = false;
    private static final int MOVEMENT_PRIORITY = 500;

    public boolean isActive() {
        return CONFIG.client.extra.killAura.enabled && isAttacking;
    }

    @Override
    public Subscription subscribeEvents() {
        return EVENT_BUS.subscribe(ClientTickEvent.class, this::handleClientTick);
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.killAura.enabled;
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().getHealth() > 0
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
            if (target != null && MODULE_MANAGER.get(PlayerSimulation.class).isOnGround()) {
                if (switchToWeapon()) {
                    isAttacking = true;
                    if (rotateTo(target)) {
                        // attack
                        attack(target);
                        delay = CONFIG.client.extra.killAura.attackDelayTicks;
                    }
                }
            } else {
                isAttacking = false;
            }
            if (isAttacking || swapping) {
                PATHING.stop(MOVEMENT_PRIORITY-1);
            }
        }
    }

    @Nullable
    private Entity findTarget() {
        for (Entity entity : CACHE.getEntityCache().getEntities().values()) {
            if (!validTarget(entity)) continue;
            if (distanceToSelf(entity) > 3.5) continue;
            return entity;
        }
        return null;
    }

    private boolean validTarget(Entity entity) {
        if (CONFIG.client.extra.killAura.targetPlayers && entity instanceof EntityPlayer player) {
            if (player.isSelfPlayer()) return false;
            return !WHITELIST_MANAGER.isUUIDFriendWhitelisted(player.getUuid())
                && !WHITELIST_MANAGER.isUUIDWhitelisted(player.getUuid())
                && !WHITELIST_MANAGER.isUUIDSpectatorWhitelisted(player.getUuid());

        } else if (entity instanceof EntityStandard e) {
            if (CONFIG.client.extra.killAura.targetHostileMobs) {
                if (hostileEntities.contains(e.getEntityType())) return true;
            }
            if (CONFIG.client.extra.killAura.targetArmorStands) {
                if (e.getEntityType() == EntityType.ARMOR_STAND) return true;
            }
        }
        return false;
    }

    @Override
    public void clientTickStopped() {
        delay = 0;
        isAttacking = false;
    }

    private void attack(final Entity entity) {
        sendClientPacketAsync(new ServerboundSwingPacket(weaponSlot == EquipmentSlot.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND));
        sendClientPacketAsync(new ServerboundInteractPacket(entity.getEntityId(), InteractAction.ATTACK, false));
    }

    private boolean rotateTo(Entity entity) {
        PATHING.rotateTowards(entity.getX(), entity.getY() + 0.2, entity.getZ(), MOVEMENT_PRIORITY);
        return true;
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
                sendClientPacketAsync(new ServerboundContainerClickPacket(0,
                                                                          actionId++,
                                                                          i,
                                                                          ContainerActionType.MOVE_TO_HOTBAR_SLOT,
                                                                          MoveToHotbarAction.SLOT_2,
                                                                          null,
                                                                          Maps.of(
                                                                              i, inventory[37],
                                                                              37, stack
                                                                          )));
                if (CACHE.getPlayerCache().getHeldItemSlot() != 1) {
                    sendClientPacketAsync(new ServerboundSetCarriedItemPacket(1));
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
        return id == 802 // netherite sword
            || id == 797 // diamond sword
            || id == 787 // gold sword
            || id == 792 // iron sword
            || id == 782 // stone sword
            || id == 777 // wooden sword
            || id == 780 // wooden axe
            || id == 785 // stone axe
            || id == 795 // iron axe
            || id == 790 // gold axe
            || id == 800 // diamond axe
            || id == 805; // netherite axe
    }

}
