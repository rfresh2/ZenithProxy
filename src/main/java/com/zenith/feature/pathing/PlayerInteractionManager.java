package com.zenith.feature.pathing;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Enchantment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EnchantmentType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.pathing.blockdata.Block;
import com.zenith.feature.pathing.blockdata.BlockState;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.MODULE;

public class PlayerInteractionManager {
    private int destroyBlockPosX = -1;
    private int destroyBlockPosY = -1;
    private int destroyBlockPosZ = -1;
    private @Nullable ItemStack destroyingItem = Container.EMPTY_STACK;
    private double destroyProgress;
    private double destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;

    private boolean sameDestroyTarget(final int x, final int y, final int z) {
        ItemStack itemStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        return x == this.destroyBlockPosX && y == this.destroyBlockPosY && z == this.destroyBlockPosZ
            && itemStack.equals(this.destroyingItem);
    }

    public boolean startDestroyBlock(final int x, final int y, final int z, Direction face) {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            Proxy.getInstance().getClient().sendAsync(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    Vector3i.from(x, y, z),
                    face,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                )
            );
            this.destroyDelay = 5;
        } else if (!this.isDestroying || !this.sameDestroyTarget(x, y, z)) {
            if (this.isDestroying) {
                Proxy.getInstance().getClient().sendAsync(
                    new ServerboundPlayerActionPacket(
                        PlayerAction.CANCEL_DIGGING,
                        Vector3i.from(this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ),
                        face,
                        CACHE.getPlayerCache().getSeqId().incrementAndGet()
                    )
                );
            }

            BlockState blockState = World.getBlockState(x, y, z);
            if (blockState.block().isAir() || blockBreakSpeed(blockState) < 1.0) {
                this.isDestroying = true;
                this.destroyBlockPosX = x;
                this.destroyBlockPosY = y;
                this.destroyBlockPosZ = z;
                this.destroyingItem = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                this.destroyProgress = 0.0;
                this.destroyTicks = 0.0F;
            }

            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    Vector3i.from(x, y, z),
                    face,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()));
        }

        return true;
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            Proxy.getInstance().getClient()
                .send(new ServerboundPlayerActionPacket(
                    PlayerAction.CANCEL_DIGGING,
                    Vector3i.from(this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ),
                    Direction.DOWN,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
            this.isDestroying = false;
            this.destroyProgress = 0.0;
        }
    }

    public boolean continueDestroyBlock(final int x, final int y, final int z, Direction directionFacing) {
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        } else if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = 5;
            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    Vector3i.from(x, y, z),
                    directionFacing,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
            return true;
        } else if (this.sameDestroyTarget(x, y, z)) {
            BlockState blockState = World.getBlockState(x, y, z);
            if (blockState.block().equals(Block.AIR)) {
                this.isDestroying = false;
                return false;
            } else {
                this.destroyProgress += blockBreakSpeed(blockState);
                ++this.destroyTicks;
                if (this.destroyProgress >= 1.0F) {
                    this.isDestroying = false;
                    Proxy.getInstance().getClient().send(
                        new ServerboundPlayerActionPacket(
                            PlayerAction.FINISH_DIGGING,
                            Vector3i.from(x, y, z),
                            directionFacing,
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.destroyDelay = 5;
                }
                return true;
            }
        } else {
            return this.startDestroyBlock(x, y, z, directionFacing);
        }
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0 ? (int)(this.destroyProgress * 10.0) : -1;
    }

    public double blockBreakSpeed(BlockState state) {
        double destroySpeed = state.block().destroySpeed();
        double toolFactor = hasCorrectToolForDrops(state) ? 30.0 : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(state);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public boolean hasCorrectToolForDrops(BlockState state) {
        if (state.block().requiredHarvestItems().isEmpty()) return true;
        var mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (mainHandStack == Container.EMPTY_STACK) return false;
        return state.block().requiredHarvestItems().contains(mainHandStack.getId());
    }

    public double getPlayerDestroySpeed(BlockState state) {
        double speed = 1.0;
        var mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (mainHandStack != Container.EMPTY_STACK) {
            speed = state.block().itemToBreakSpeedMap().getOrDefault(mainHandStack.getId(), 1.0);
        }

        if (speed > 1.0F && mainHandStack != Container.EMPTY_STACK) {
            var enchantments = mainHandStack.getEnchantments();
            var efficiencyLevel = new AtomicInteger(0);
            for (Enchantment e : enchantments) {
                if (e.type() == EnchantmentType.EFFICIENCY) {
                    efficiencyLevel.set(e.level());
                    break;
                }
            }
            var lvl = efficiencyLevel.get();
            if (lvl > 0) {
                speed += (float)(lvl * lvl + 1);
            }
        }

        boolean hasDigSpeedEffect = false;
        int hasteAmplifier = 0;
        var hasteEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.HASTE);
        if (hasteEffect != null) {
            hasDigSpeedEffect = true;
            hasteAmplifier = hasteEffect.getAmplifier();
        }
        int conduitPowerAmplifier = 0;
        var conduitPowerEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.CONDUIT_POWER);
        if (conduitPowerEffect != null) {
            hasDigSpeedEffect = true;
            conduitPowerAmplifier = conduitPowerEffect.getAmplifier();
        }

        if (hasDigSpeedEffect) {
            int digSpeedAmplification = Math.max(hasteAmplifier, conduitPowerAmplifier);
            speed *= 1.0 + (digSpeedAmplification + 1) * 0.2;
        }

        var miningFatigueEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.MINING_FATIGUE);

        if (miningFatigueEffect != null) {
            speed *= switch(miningFatigueEffect.getAmplifier()) {
                case 0 -> 0.3;
                case 1 -> 0.09;
                case 2 -> 0.0027;
                default -> 8.1E-4;
            };
        }

        boolean isEyeInWater = World.isWater(
            World.getBlockAtBlockPos(
                MathHelper.floorI(CACHE.getPlayerCache().getX()), MathHelper.floorI(CACHE.getPlayerCache().getEyeY()), MathHelper.floorI(CACHE.getPlayerCache().getZ())));
        if (isEyeInWater) {
            boolean hasAquaAffinity = false;
            if (mainHandStack != Container.EMPTY_STACK) {
                for (Enchantment e : mainHandStack.getEnchantments()) {
                    if (e.type().equals(EnchantmentType.AQUA_AFFINITY)) {
                        hasAquaAffinity = true;
                        break;
                    }
                }
            }
            if (!hasAquaAffinity) speed /= 5.0;
        }

        if (!MODULE.get(PlayerSimulation.class).isOnGround()) { // todo: cache
            speed /= 5.0;
        }

        return speed;
    }
}
