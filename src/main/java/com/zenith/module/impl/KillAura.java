package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.module.ClientTickEvent;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static com.zenith.Shared.*;

public class KillAura extends AbstractInventoryModule {

    private static final Set<EntityType> hostileEntities = ReferenceOpenHashSet.of(
        EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN,
        EntityType.ENDER_DRAGON, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.GHAST, EntityType.GUARDIAN,
        EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.FIREBALL, EntityType.MAGMA_CUBE,
        EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER,
        EntityType.SHULKER, EntityType.SHULKER_BULLET, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME,
        EntityType.SMALL_FIREBALL, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR,
        EntityType.WARDEN, EntityType.WITCH, EntityType.WITHER, EntityType.ZOGLIN, EntityType.ZOMBIE,
        EntityType.ZOMBIE_VILLAGER
    );
    private static final Set<EntityType> neutralEntities = ReferenceOpenHashSet.of(
        EntityType.BEE, EntityType.DOLPHIN, EntityType.ENDERMAN, EntityType.FOX, EntityType.GOAT, EntityType.IRON_GOLEM,
        EntityType.LLAMA, EntityType.PANDA, EntityType.POLAR_BEAR, EntityType.TRADER_LLAMA, EntityType.WOLF,
        EntityType.ZOMBIFIED_PIGLIN
    );
    private int delay = 0;
    private boolean isAttacking = false;
    private EquipmentSlot weaponSlot = EquipmentSlot.MAIN_HAND;
    private static final int MOVEMENT_PRIORITY = 500;
    private final IntList swords = ITEMS_MANAGER.getItemsContaining("_sword");
    private final IntList axes = ITEMS_MANAGER.getItemsContaining("_axe");

    public KillAura() {
        super(false, 1, MOVEMENT_PRIORITY);
    }

    public boolean isActive() {
        return CONFIG.client.extra.killAura.enabled && isAttacking;
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, ClientTickEvent.class, this::handleClientTick);
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.killAura.enabled;
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && !MODULE_MANAGER.get(AutoEat.class).isEating()) {
            if (delay > 0) {
                delay--;
                return;
            }
            // find non-friended players or hostile mobs within 3.5 blocks
            final Entity target = findTarget();
            // rotate to target
            if (target != null && MODULE_MANAGER.get(PlayerSimulation.class).isOnGround()) {
                isAttacking = true;
                if (switchToWeapon()) {
                    if (rotateTo(target)) {
                        attack(target);
                        delay = CONFIG.client.extra.killAura.attackDelayTicks;
                    }
                }
                PATHING.stop(MOVEMENT_PRIORITY-1);
            } else {
                isAttacking = false;
            }
        }
    }

    @Nullable
    private Entity findTarget() {
        for (Entity entity : CACHE.getEntityCache().getEntities().values()) {
            if (!validTarget(entity)) continue;
            if (CACHE.getPlayerCache().distanceToSelf(entity) > CONFIG.client.extra.killAura.attackRange) continue;
            return entity;
        }
        return null;
    }

    private boolean validTarget(Entity entity) {
        if (CONFIG.client.extra.killAura.targetPlayers && entity instanceof EntityPlayer player) {
            if (player.isSelfPlayer()) return false;
            return !PLAYER_LISTS.getFriendsList().contains(player.getUuid())
                && !PLAYER_LISTS.getWhitelist().contains(player.getUuid())
                && !PLAYER_LISTS.getSpectatorWhitelist().contains(player.getUuid());

        } else if (entity instanceof EntityStandard e) {
            if (CONFIG.client.extra.killAura.targetHostileMobs) {
                if (hostileEntities.contains(e.getEntityType())) return true;
            }
            if (CONFIG.client.extra.killAura.targetArmorStands) {
                if (e.getEntityType() == EntityType.ARMOR_STAND) return true;
            }
            if (CONFIG.client.extra.killAura.targetNeutralMobs) {
                if (neutralEntities.contains(e.getEntityType())) {
                    if (CONFIG.client.extra.killAura.onlyNeutralAggressive) {
                        // https://wiki.vg/Entity_metadata#Mob
                        var byteMetadata = getMetadataFromId(entity.getMetadata(), 15);
                        if (byteMetadata == null) return false;
                        if (byteMetadata instanceof ByteEntityMetadata byteData) {
                            var data = byteData.getPrimitiveValue() & 0x04;
                            return data != 0;
                        }
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private EntityMetadata getMetadataFromId(List<EntityMetadata> metadata, int id) {
        for (int i = 0; i < metadata.size(); i++) {
            if (metadata.get(i).getId() == id) {
                return metadata.get(i);
            }
        }
        return null;
    }

    @Override
    public void clientTickStopped() {
        delay = 0;
        isAttacking = false;
    }

    private void attack(final Entity entity) {
        sendClientPacketsAsync(
            new ServerboundInteractPacket(entity.getEntityId(), InteractAction.ATTACK, false),
            new ServerboundSwingPacket(weaponSlot == EquipmentSlot.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND)
        );
    }

    private boolean rotateTo(Entity entity) {
        PATHING.rotateTowards(entity.getX(), entity.getY() + 0.2, entity.getZ(), MOVEMENT_PRIORITY);
        // todo: add a raytrace check to test if we can hit the target
        return true;
    }

    public boolean switchToWeapon() {
        if (!CONFIG.client.extra.killAura.switchWeapon) return true;
        delay = doInventoryActions();
        var hand = getHand();
        weaponSlot = hand == Hand.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.MAIN_HAND;
        return delay == 0;
    }

    private boolean isWeapon(int id) {
        return swords.contains(id) || axes.contains(id);
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        return isWeapon(itemStack.getId());
    }
}
