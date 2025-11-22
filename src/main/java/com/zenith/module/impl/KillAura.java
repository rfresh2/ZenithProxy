package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.*;
import com.zenith.feature.player.raycast.RaycastHelper;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ToolTier;
import com.zenith.mc.item.ToolType;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.cloudburstmc.math.vector.Vector2f;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class KillAura extends AbstractInventoryModule {

    private static final Set<EntityType> hostileEntities = ReferenceOpenHashSet.of(
        EntityType.BLAZE, EntityType.BOGGED, EntityType.BREEZE, EntityType.CAVE_SPIDER, EntityType.CREAKING,
        EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN,
        EntityType.ENDER_DRAGON, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.GHAST, EntityType.GUARDIAN,
        EntityType.HOGLIN, EntityType.HUSK, EntityType.ILLUSIONER, EntityType.FIREBALL, EntityType.MAGMA_CUBE,
        EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER,
        EntityType.SHULKER, EntityType.SHULKER_BULLET, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME,
        EntityType.SMALL_FIREBALL, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR,
        EntityType.WARDEN, EntityType.WITCH, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOGLIN,
        EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER
    );
    private static final Set<EntityType> neutralEntities = ReferenceOpenHashSet.of(
        EntityType.BEE, EntityType.DOLPHIN, EntityType.ENDERMAN, EntityType.FOX, EntityType.GOAT, EntityType.IRON_GOLEM,
        EntityType.LLAMA, EntityType.PANDA, EntityType.POLAR_BEAR, EntityType.TRADER_LLAMA, EntityType.WOLF,
        EntityType.ZOMBIFIED_PIGLIN
    );
    private int delay = 0;
    private final WeakReference<EntityLiving> nullRef = new WeakReference<>(null);
    private WeakReference<EntityLiving> attackTarget = nullRef;

    public KillAura() {
        super(HandRestriction.MAIN_HAND, 1);
        // convert legacy config
        if (CONFIG.client.extra.killAura.targetArmorStands) {
            if (!CONFIG.client.extra.killAura.customTargets.contains(EntityType.ARMOR_STAND)) {
                CONFIG.client.extra.killAura.customTargets.add(EntityType.ARMOR_STAND);
            }
            CONFIG.client.extra.killAura.targetArmorStands = false;
            CONFIG.client.extra.killAura.targetCustom = true;
            saveConfigAsync();
        }
    }

    public boolean isActive() {
        return CONFIG.client.extra.killAura.enabled && attackTarget.get() != null;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTick),
            of(ClientBotTick.Stopped.class, this::handleBotTickStopped)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.killAura.enabled;
    }

    @Override
    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.killAura.actionPriority, 8000);
    }

    @Override
    public void onDisable() {
        delay = 0;
        attackTarget = nullRef;
    }

    private void handleClientTick(final ClientBotTick event) {
        if (delay > 0) {
            delay--;
            EntityLiving target = attackTarget.get();
            if (target != null && canPossiblyReach(target)) {
                if (!hasRotation(target)) {
                    rotateTo(target);
                }
                INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority() - 1));
            }
            return;
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()) {
            final EntityLiving target = findTarget();
            if (target != null) {
                if (!attackTarget.refersTo(target))
                    attackTarget = new WeakReference<>(target);
                if (switchToWeapon()) {
                    INVENTORY.submit(InventoryActionRequest.noAction(this, getPriority() - 1));
                    attack(target).addInputExecutedListener(this::onAttackInputExecuted);
                } else {
                    // stop while doing inventory actions
                    INPUTS.submit(InputRequest.builder()
                        .owner(this)
                        .priority(getPriority() - 1)
                        .build());
                }
                return;
            }
        }
        attackTarget = nullRef;
    }


    private void onAttackInputExecuted(InputRequestFuture future) {
        if (future.getClickResult() instanceof ClickResult.LeftClickResult leftClickResult
            && leftClickResult.getEntity() != null && leftClickResult.getEntity() == attackTarget.get()) {
            delay = computeAttackDelayTicks();
        }
    }

    private int computeAttackDelayTicks() {
        var delay = CONFIG.client.extra.killAura.attackDelayTicks;
        if (!CONFIG.client.extra.killAura.tpsSync) return delay;
        // Scale delay by server slowdown: at 10 TPS, wait twice as long; at 20 TPS, unchanged.
        return MathHelper.ceilI(delay * (20.0 / MathHelper.clamp(TPS.getTPSValue(), 1.0, 20.0)));
    }

    @Nullable
    private EntityLiving findTarget() {
        // todo: check if its worth it to copy the entity cache to a list to avoid streams
        var entityStream = CACHE.getEntityCache().getEntities().values().stream()
            .filter(e -> e instanceof EntityLiving)
            .map(e -> (EntityLiving) e)
            .filter(e -> e != CACHE.getPlayerCache().getThePlayer())
            .filter(EntityLiving::isAlive);
        entityStream = switch (CONFIG.client.extra.killAura.priority) {
            case NONE -> entityStream;
            case NEAREST -> entityStream
                .sorted(Comparator.comparingDouble(e -> CACHE.getPlayerCache().distanceSqToSelf(e)));
        };

        return entityStream
            .filter(this::validTarget)
            .filter(this::canPossiblyReach)
            .findFirst()
            .orElse(null);
    }

    private boolean validTarget(EntityLiving entity) {
        if (CONFIG.client.extra.killAura.targetPlayers && entity instanceof EntityPlayer player) {
            if (player.isSelfPlayer()) return false;
            return !PLAYER_LISTS.getFriendsList().contains(player.getUuid())
                && !(PLAYER_LISTS.getSpawnPatrolIgnoreList().contains(player.getUuid()) && MODULE.get(SpawnPatrol.class).isEnabled())
                && !PLAYER_LISTS.getWhitelist().contains(player.getUuid())
                && !PLAYER_LISTS.getSpectatorWhitelist().contains(player.getUuid());

        } else if (entity instanceof EntityStandard e) {
            if (CONFIG.client.extra.killAura.targetCustom) {
                if (CONFIG.client.extra.killAura.customTargets.contains(e.getEntityType())) {
                    return true;
                }
            }
            if (CONFIG.client.extra.killAura.targetHostileMobs) {
                if (hostileEntities.contains(e.getEntityType())) {
                    if (CONFIG.client.extra.killAura.onlyHostileAggressive) {
                        if (isAggressive(e)) return true;
                    } else {
                        return true;
                    }
                }
            }
            if (CONFIG.client.extra.killAura.targetNeutralMobs) {
                if (neutralEntities.contains(e.getEntityType())) {
                    if (CONFIG.client.extra.killAura.onlyNeutralAggressive) {
                        if (isAggressive(e)) return true;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isAggressive(final EntityLiving entity) {
        // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Mob
        var byteData = entity.getMetadataValue(15, MetadataTypes.BYTE, Byte.class);
        if (byteData == null) return false;
        return (byteData & 0x04) != 0;
    }

    private void handleBotTickStopped(final ClientBotTick.Stopped event) {
        delay = 0;
        attackTarget = nullRef;
    }

    private InputRequestFuture attack(final EntityLiving entity) {
        var rotation = getRotationTo(entity);
        if (rotation == null) return InputRequestFuture.rejected;
        return INPUTS.submit(InputRequest.builder()
            .owner(this)
            .input(Input.builder()
                .leftClick(true)
                .clickRequiresRotation(false) // any rotation is fine, as long as click target is met
                .clickTarget(new ClickTarget.EntityInstance(entity))
                .build())
            .yaw(rotation.getX())
            .pitch(rotation.getY())
            .priority(getPriority())
            .build());
    }

    private void rotateTo(EntityLiving entity) {
        var rotation = getRotationTo(entity);
        if (rotation == null) return;
        INPUTS.submit(InputRequest.builder()
            .owner(this)
            .yaw(rotation.getX())
            .pitch(rotation.getY())
            .priority(getPriority())
            .build());
    }

    private boolean hasRotation(final EntityLiving entity) {
        var entityRaycastResult = RaycastHelper.playerEyeRaycastThroughToTarget(entity);
        return entityRaycastResult.hit();
    }

    private @Nullable Vector2f getRotationTo(final EntityLiving entity) {
        if (CONFIG.client.extra.killAura.raycast) {
            return raycastRotationTo(entity);
        } else {
            return RotationHelper.shortestRotationTo(entity);
        }
    }

    private @Nullable Vector2f raycastRotationTo(final EntityLiving entity) {
        var currentRaycast = RaycastHelper.blockOrEntityRaycastFromPos(
            BOT.getX(),
            BOT.getEyeY(),
            BOT.getZ(),
            BOT.getYaw(),
            BOT.getPitch(),
            BOT.getBlockReachDistance(),
            BOT.getEntityInteractDistance()
        );
        if (currentRaycast.hit() && currentRaycast.isEntity() && currentRaycast.entity().entity() == entity) {
            return Vector2f.from(BOT.getYaw(), BOT.getPitch());
        }

        var dimensions = entity.dimensions();
        double halfW = dimensions.getX() / 2.0;
        double halfH = dimensions.getY() / 2.0;
        double entityCenterX = entity.getX();
        double entityMinX = entityCenterX - halfW;
        double entityMaxX = entityCenterX + halfW;
        double entityCenterY = entity.getY() + halfH;
        double entityMinY = entity.getY();
        double entityMaxY = entity.getY() + dimensions.getY();
        double entityCenterZ = entity.getZ();
        double entityMinZ = entityCenterZ - halfW;
        double entityMaxZ = entityCenterZ + halfW;

        var entityCenterRotation = RotationHelper.rotationTo(entityCenterX, entityCenterY, entityCenterZ);
        var centerRaycast = RaycastHelper.blockOrEntityRaycastFromPos(
            BOT.getX(),
            BOT.getEyeY(),
            BOT.getZ(),
            entityCenterRotation.getX(),
            entityCenterRotation.getY(),
            BOT.getBlockReachDistance(),
            BOT.getEntityInteractDistance()
        );
        if (centerRaycast.hit() && centerRaycast.isEntity() && centerRaycast.entity().entity() == entity) {
            return entityCenterRotation;
        }

        double step = 0.1;
        double maxStep = Math.max(halfW, halfH);
        for (double d = step; d <= maxStep; d += step) {
            for (double dx = -1; dx <= 1; dx++) {
                for (double dy = -1; dy <= 1; dy++) {
                    for (double dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        double x = MathHelper.clamp(entityCenterX + (dx * d), entityMinX, entityMaxX);
                        double y = MathHelper.clamp(entityCenterY + (dy * d), entityMinY, entityMaxY);
                        double z = MathHelper.clamp(entityCenterZ + (dz * d), entityMinZ, entityMaxZ);
                        var rotation = RotationHelper.rotationTo(x, y, z);
                        var raycast = RaycastHelper.blockOrEntityRaycastFromPos(
                            BOT.getX(),
                            BOT.getEyeY(),
                            BOT.getZ(),
                            rotation.getX(),
                            rotation.getY(),
                            BOT.getBlockReachDistance(),
                            BOT.getEntityInteractDistance()
                        );
                        if (raycast.hit() && raycast.isEntity() && raycast.entity().entity() == entity) {
                            return rotation;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean canPossiblyReach(final EntityLiving entity) {
        var rangeSq = Math.pow(BOT.getEntityInteractDistance(), 2) + 5;
        if (CACHE.getPlayerCache().distanceSqToSelf(entity) > rangeSq) return false;
        var rotation = getRotationTo(entity);
        if (rotation == null) return false;
        var entityRaycastResult = RaycastHelper.playerEyeRaycastThroughToTarget(entity, rotation.getX(), rotation.getY());
        return entityRaycastResult.hit();
    }

    public boolean switchToWeapon() {
        if (!CONFIG.client.extra.killAura.switchWeapon) return true;
        delay = doInventoryActions();
        return delay == 0;
    }

    private boolean isWeapon(int id) {
        var itemData = ItemRegistry.REGISTRY.get(id);
        if (itemData == null) return false;
        var toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        boolean typeMatch = switch (CONFIG.client.extra.killAura.weaponType) {
            case ANY -> toolTag.type() == ToolType.SWORD || toolTag.type() == ToolType.AXE;
            case SWORD -> toolTag.type() == ToolType.SWORD;
            case AXE -> toolTag.type() == ToolType.AXE;
        };
        if (!typeMatch) return false;
        return switch (CONFIG.client.extra.killAura.weaponMaterial) {
            case ANY -> toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.NETHERITE;
            case DIAMOND -> toolTag.tier() == ToolTier.DIAMOND;
            case NETHERITE -> toolTag.tier() == ToolTier.NETHERITE;
        };
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        return isWeapon(itemStack.getId());
    }
}
