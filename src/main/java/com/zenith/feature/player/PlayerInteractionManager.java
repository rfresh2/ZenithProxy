package com.zenith.feature.player;

import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.player.raycast.BlockRaycastResult;
import com.zenith.feature.player.raycast.EntityRaycastResult;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.BlockTags;
import com.zenith.mc.block.Direction;
import com.zenith.mc.enchantment.EnchantmentData;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ToolTag;
import com.zenith.mc.item.ToolTier;
import com.zenith.util.math.MathHelper;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static com.zenith.Globals.*;

@Getter
public class PlayerInteractionManager {
    private int destroyBlockPosX = -1;
    private int destroyBlockPosY = -1;
    private int destroyBlockPosZ = -1;
    private @Nullable ItemStack destroyingItem = Container.EMPTY_STACK;
    private double destroyProgress;
    private double destroyTicks;
    private int destroyDelay;
    private final int destroyDelayInterval = 6;
    private boolean isDestroying;

    private boolean sameDestroyTarget(final int x, final int y, final int z) {
        ItemStack itemStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        return x == this.destroyBlockPosX && y == this.destroyBlockPosY && z == this.destroyBlockPosZ
            && Objects.equals(this.destroyingItem, itemStack);
    }

    public boolean isDestroying(final int x, final int y, final int z) {
        return this.isDestroying && this.sameDestroyTarget(x, y, z);
    }

    protected boolean startDestroyBlock(final int x, final int y, final int z, Direction face) {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            BOT.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Creative break", System.currentTimeMillis(), x, y, z);
            Proxy.getInstance().getClient().sendAsync(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    face.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                )
            );
            destroyBlock(x, y, z);
            this.destroyDelay = destroyDelayInterval;
        } else if (!this.isDestroying || !this.sameDestroyTarget(x, y, z)) {
            if (this.isDestroying) {
                BOT.debug("[{}] [{}, {}, {}] StartDestroyBlock CANCEL: Changed destroy target", System.currentTimeMillis(), x, y, z);
                Proxy.getInstance().getClient().sendAsync(
                    new ServerboundPlayerActionPacket(
                        PlayerAction.ABORT_DESTROY_BLOCK,
                        this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                        face.mcpl(),
                        0
                    )
                );
            }

            Block block = World.getBlock(x, y, z);
            if (!BLOCK_DATA.isAir(block) && blockBreakSpeed(block) >= 1.0) {
                destroyBlock(x, y, z);
                BOT.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Instant break", System.currentTimeMillis(), x, y, z);
            } else {
                this.isDestroying = true;
                this.destroyBlockPosX = x;
                this.destroyBlockPosY = y;
                this.destroyBlockPosZ = z;
                this.destroyingItem = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                this.destroyProgress = 0.0;
                this.destroyTicks = 0.0F;
                BOT.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Start multi-tick break", System.currentTimeMillis(), x, y, z);
            }

            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    face.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()));
        }

        return true;
    }

    protected void stopDestroyBlock() {
        if (this.isDestroying) {
            BOT.debug("[{}] [{}, {}, {}] StopDestroyBlock CANCEL", System.currentTimeMillis(), this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ);
            Proxy.getInstance().getClient()
                .send(new ServerboundPlayerActionPacket(
                    PlayerAction.ABORT_DESTROY_BLOCK,
                    this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                    Direction.DOWN.mcpl(),
                    0
                ));
        }
        this.isDestroying = false;
        this.destroyProgress = 0;
    }

    protected boolean continueDestroyBlock(final int x, final int y, final int z, Direction directionFacing) {
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        } else if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = destroyDelayInterval;
            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    directionFacing.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
            destroyBlock(x, y, z);
            BOT.debug("[{}] [{}, {}, {}] ContinueDestroyBlock START: Creative Break", System.currentTimeMillis(), x, y, z);
            return true;
        } else if (this.sameDestroyTarget(x, y, z)) {
            Block block = World.getBlock(x, y, z);
            if (BLOCK_DATA.isAir(block)) {
                this.isDestroying = false;
                return false;
            } else {
                this.destroyProgress += blockBreakSpeed(block);
                ++this.destroyTicks;
                if (this.destroyProgress >= 1.0F) {
                    this.isDestroying = false;
                    Proxy.getInstance().getClient().send(
                        new ServerboundPlayerActionPacket(
                            PlayerAction.STOP_DESTROY_BLOCK,
                            x, y, z,
                            directionFacing.mcpl(),
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    destroyBlock(x, y, z);
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.destroyDelay = destroyDelayInterval;
                    BOT.debug("[{}] [{}, {}, {}] ContinueDestroyBlock FINISH", System.currentTimeMillis(), x, y, z);
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

    public double blockBreakSpeed(Block block) {
        double destroySpeed = block.destroySpeed();
        double toolFactor = hasCorrectToolForDrops(block, CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND))
            ? 30.0
            : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(block);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public double blockBreakSpeed(Block block, ItemStack item) {
        double destroySpeed = block.destroySpeed();
        double toolFactor = hasCorrectToolForDrops(block, item) ? 30.0 : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(block);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public boolean hasCorrectToolForDrops(Block block, ItemStack item) {
        if (!block.requiresCorrectToolForDrops()) return true;
        if (item == Container.EMPTY_STACK) return false;
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        var blockTags = block.blockTags();
        if (!blockTags.contains(toolTag.type().getBlockTag())) return false;
        if (blockTags.contains(BlockTags.NEEDS_STONE_TOOL)) {
            return toolTag.tier() == ToolTier.STONE || toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_IRON_TOOL)) {
            return toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        return true;
    }

    public boolean matchingTool(ItemStack item, Block block) {
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        return block.blockTags().contains(toolTag.type().getBlockTag());
    }

    public int getEnchantmentLevel(ItemStack item, EnchantmentData enchantmentData) {
        if (item == Container.EMPTY_STACK) return 0;
        DataComponents dataComponents = item.getDataComponents();
        if (dataComponents == null) return 0;
        ItemEnchantments itemEnchantments = dataComponents.get(DataComponentTypes.ENCHANTMENTS);
        if (itemEnchantments == null) return 0;
        if (!itemEnchantments.getEnchantments().containsKey(enchantmentData.id())) return 0;
        return itemEnchantments.getEnchantments().get(enchantmentData.id());
    }

    public double getPlayerDestroySpeed(Block block) {
        double speed = 1.0;
        var mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (mainHandStack != Container.EMPTY_STACK) {
            if (matchingTool(mainHandStack, block)) {
                ItemData itemData = ItemRegistry.REGISTRY.get(mainHandStack.getId());
                ToolTag toolTag = itemData.toolTag();
                speed = toolTag.tier().getSpeed();
            }
        }

        if (speed > 1.0) {
            float miningEfficiencyAttribute = BOT
                .getAttributeValue(AttributeType.Builtin.MINING_EFFICIENCY, 0);
            speed += miningEfficiencyAttribute;
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

        speed *= BOT.getAttributeValue(AttributeType.Builtin.BLOCK_BREAK_SPEED, 1.0f);

        boolean isEyeInWater = World.isWater(
            World.getBlock(
                MathHelper.floorI(BOT.getX()),
                MathHelper.floorI(BOT.getEyeY()),
                MathHelper.floorI(BOT.getZ())));
        if (isEyeInWater) {
            speed *= BOT.getAttributeValue(AttributeType.Builtin.SUBMERGED_MINING_SPEED, 0.2f);
        }

        if (!BOT.isOnGround()) {
            speed /= 5.0;
        }

        return speed;
    }

    private void destroyBlock(int x, int y, int z) {
        CACHE.getChunkCache().getChunkSection(x, y, z)
            .setBlock(x & 15, y & 15, z & 15, BlockRegistry.AIR.id());
    }

    protected InteractionResult interact(Hand hand, EntityRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundInteractPacket(
            ray.entity().getEntityId(),
            InteractAction.INTERACT,
            0, 0, 0,
            hand,
            BOT.isSneaking()
        ));
        return InteractionResult.PASS;
    }

    protected InteractionResult interactAt(Hand hand, EntityRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundInteractPacket(
            ray.entity().getEntityId(),
            InteractAction.INTERACT_AT,
            0, 0, 0,
            hand,
            BOT.isSneaking()
        ));
        return InteractionResult.PASS;
    }

    protected InteractionResult useItemOn(Hand hand, BlockRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundUseItemOnPacket(
            ray.x(), ray.y(), ray.z(),
            ray.direction().mcpl(),
            hand,
            // todo: cursor raytrace
            0, 0, 0,
            false,
            false,
            CACHE.getPlayerCache().getSeqId().incrementAndGet()
        ));
        // todo: check if we are placing a block
        //  if so, add the block to the world so we don't have a brief desync
        return InteractionResult.PASS;
    }

    // todo: is this allowed if we are not holding a usable item? or any item at all?
    protected InteractionResult useItem(Hand hand) {
        Proxy.getInstance().getClient().send(new ServerboundUseItemPacket(
            hand,
            CACHE.getPlayerCache().getSeqId().incrementAndGet(),
            BOT.getYaw(),
            BOT.getPitch()
        ));
        return InteractionResult.PASS;
    }

    protected void attackEntity(final EntityRaycastResult entity) {
        BOT.debug("[{}] [{}, {}, {}] Attack Entity", System.currentTimeMillis(), entity.entity().getX(), entity.entity().getY(), entity.entity().getZ());
        Proxy.getInstance().getClient().sendAsync(new ServerboundInteractPacket(entity.entity().getEntityId(), InteractAction.ATTACK, false));
    }

    protected void releaseUsingItem() {
        Proxy.getInstance().getClient().sendAsync(new ServerboundPlayerActionPacket(
            PlayerAction.RELEASE_USE_ITEM,
            0, 0, 0,
            Direction.DOWN.mcpl(),
            CACHE.getPlayerCache().getSeqId().incrementAndGet()
        ));
    }
}
