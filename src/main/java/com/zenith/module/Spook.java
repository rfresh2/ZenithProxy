package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.util.TickTimer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.isNull;

public class Spook extends Module {
    public final AtomicBoolean hasTarget;
    private final TickTimer stareTimer;

    public Spook(Proxy proxy) {
        super(proxy);
        this.stareTimer = new TickTimer();
        this.hasTarget = new AtomicBoolean(false);
    }

    public static float getPitch(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        double y = entity.getY() + 1.6; // eye height
        double diffX = entity.getX() - player.getX();
        double diffY = y - (player.getY() + 1.6);
        double diffZ = entity.getZ() - player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return player.getPitch() + wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - player.getPitch());
    }

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.spook.enabled && isNull(this.proxy.getCurrentPlayer().get()) && !proxy.isInQueue()) {
            stareTick();
        } else {
            hasTarget.lazySet(false);
        }
    }

    public static float getYaw(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        return player.getYaw() + wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - player.getZ(), entity.getX() - player.getX())) - 90f - player.getYaw());
    }

    private void stareTick() {
        if (stareTimer.tick(CONFIG.client.extra.spook.tickDelay, true)) {
            final Optional<EntityPlayer> nearestPlayer = getNearestPlayer();
            if (nearestPlayer.isPresent()) {
                this.hasTarget.set(true);
                this.proxy.getClient().send(new ClientPlayerRotationPacket(
                        true,
                        getYaw(nearestPlayer.get()),
                        getPitch(nearestPlayer.get())
                ));
            } else {
                this.hasTarget.set(false);
            }
        }
    }

    public static float wrapDegrees(float degrees) {
        float f = degrees % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    private Optional<EntityPlayer> getNearestPlayer() {
        return CACHE.getEntityCache().getEntities().values().stream()
                .filter(entity -> entity instanceof EntityPlayer)
                .map(entity -> (EntityPlayer) entity)
                .filter(e -> e != CACHE.getPlayerCache().getThePlayer())
                .min((e1, e2) -> getDistanceToPlayer(e1) - getDistanceToPlayer(e2));
    }

    private double getDistance(final EntityPlayer p1, final EntityPlayer p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2) + Math.pow(p2.getZ() - p1.getZ(), 2));
    }

    private int getDistanceToPlayer(final EntityPlayer e) {
        return (int) getDistance(e, CACHE.getPlayerCache().getThePlayer());
    }
}
