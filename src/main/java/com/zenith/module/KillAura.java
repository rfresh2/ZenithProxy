package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.google.common.collect.Sets;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityMob;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientTickEvent;
import lombok.Getter;
import net.daporkchop.lib.math.vector.Vec3d;

import java.util.Optional;
import java.util.Set;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;

public class KillAura extends Module {

    private static final Set<MobType> friendlyMobTypes = Sets.newHashSet(
            MobType.SKELETON_HORSE, MobType.ZOMBIE_HORSE, MobType.ARMOR_STAND, MobType.DONKEY, MobType.MULE, MobType.ZOMBIE_PIGMAN,
            MobType.BAT, MobType.PIG, MobType.SHEEP, MobType.COW, MobType.CHICKEN, MobType.SQUID, MobType.WOLF,
            MobType.MOOSHROOM, MobType.OCELOT, MobType.IRON_GOLEM, MobType.HORSE, MobType.RABBIT, MobType.POLAR_BEAR,
            MobType.LLAMA, MobType.PARROT, MobType.VILLAGER
    );
    private int delay = 0;
    @Getter
    private boolean isAttacking = false;

    @Subscribe
    public void handleClientTick(final ClientTickEvent event) {
        if (CONFIG.client.extra.killAura.enabled
                && !Proxy.getInstance().isInQueue()
                && isNull(Proxy.getInstance().getCurrentPlayer().get())
                && !MODULE_MANAGER.getModule(AutoEat.class).map(AutoEat::isEating).orElse(false)) {
            if (delay > 0) {
                delay--;
                return;
            }
            // find non-friended players or hostile mobs within 3.5 blocks
            final Optional<Entity> target = CACHE.getEntityCache().getEntities().values().stream()
                    .filter(entity -> entity instanceof EntityPlayer || entity instanceof EntityMob)
                    .filter(entity -> CONFIG.client.extra.killAura.targetPlayers || !(entity instanceof EntityPlayer))
                    .filter(entity -> CONFIG.client.extra.killAura.targetMobs || !(entity instanceof EntityMob))
                    .filter(entity -> !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSelfPlayer()))
                    .filter(entity -> distanceToSelf(entity) <= 3.5)
                    // filter friends
                    .filter(entity -> !(entity instanceof EntityPlayer
                            && CACHE.getTabListCache().getTabList().get(entity.getUuid())
                            .map(p -> CONFIG.client.extra.friendList.stream().anyMatch(n -> n.equalsIgnoreCase(p.getName())))
                            .orElse(false)))
                    // filter whitelist
                    .filter(entity -> !(entity instanceof EntityPlayer)
                            || (!WHITELIST_MANAGER.isUUIDWhitelisted(entity.getUuid())
                            && !WHITELIST_MANAGER.isUUIDSpectatorWhitelisted(entity.getUuid())))
                    .filter(entity -> {
                        if (entity instanceof EntityMob && CONFIG.client.extra.killAura.avoidFriendlyMobs) {
                            return !friendlyMobTypes.contains(((EntityMob) entity).getMobType());
                        }
                        return true;
                    })
                    .findFirst();

            // rotate to target
            if (target.isPresent()) {
                isAttacking = true;
                if (rotateTo(target.get())) {
                    // attack
                    attack(target.get());
                    delay = 5;
                }
            } else {
                isAttacking = false;
            }
        }
    }

    @Override
    public void clientTickStopping() {
        delay = 0;
        isAttacking = false;
    }

    private void attack(final Entity entity) {
        Proxy.getInstance().getClient().send(new ClientPlayerInteractEntityPacket(entity.getEntityId(), InteractAction.ATTACK));
        Proxy.getInstance().getClient().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
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
        if (!((currentYaw + 0.05 > yaw && currentYaw - 0.05 < yaw) && (currentPitch + 0.05 > pitch && currentPitch - 0.05 < pitch))) {
            Proxy.getInstance().getClient().send(new ClientPlayerRotationPacket(false, (float) yaw, (float) pitch));
            delay = 1;
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

}
