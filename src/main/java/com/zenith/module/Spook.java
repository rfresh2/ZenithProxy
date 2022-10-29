package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.event.proxy.NewPlayerInVisualRangeEvent;
import com.zenith.util.TickTimer;

import java.util.*;

import static com.zenith.util.Constants.*;
import static com.zenith.util.Constants.CACHE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Spook extends Module {
    private final TickTimer stareTimer = new TickTimer();

    List<Integer> focusIds = new ArrayList<>();

//    private static Map<Integer, Double> poseHeights = ImmutableMap.of(0, 1.6, 2, 1.5, -128, 0.5);
    public Spook(Proxy proxy) {
        super(proxy);
    }

    @Subscribe
    public void handleClientTickEvent(final ClientTickEvent event) {
        if (CONFIG.client.extra.spook.enabled && isNull(this.proxy.getCurrentPlayer().get()) && !proxy.isInQueue()) {
            stareTick();
        }
    }

    private void stareTick() {
        if (stareTimer.tick(CONFIG.client.extra.spook.tickDelay, true)) {
            Entity focus = getFocus();
            if (nonNull(focus)) {
//                System.out.println(focus.getUuid());
                this.proxy.getClient().send(new ClientPlayerRotationPacket(
                        true,
                        getYaw(focus),
                        getPitch(focus)
                ));
            }
        }
    }
    
    public static float getYaw(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        return player.getYaw() + wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - player.getZ(), entity.getX() - player.getX())) - 90f - player.getYaw());
    }

//    public static double getEyeHeight(Entity entity) {
//        return poseHeights.get(entity.getEntityMetadataAsArray()[0].getValue());
//    }

    public static float getPitch(Entity entity) {
        PlayerCache player = CACHE.getPlayerCache();
        double y;
        y = entity.getY() + 1.6; // eye height
//        if (entity.getProperties().)
//        System.out.println(Arrays.stream(entity.getEntityMetadataAsArray()).filter(e -> e.getId() == 18).findFirst());
//        System.out.println(entity.getEntityMetadataAsArray()[0].getValue());
        double diffX = entity.getX() - player.getX();
//        double diffY = y - (player.getY() + getEyeHeight(entity));
        double diffY = y - (player.getY() + 1.6);
        double diffZ = entity.getZ() - player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return player.getPitch() + wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - player.getPitch());
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
    private Entity getFocus() {
        Map<Integer, Entity> entityMap = CACHE.getEntityCache().getEntities();
        if (focusIds.isEmpty()) { return null; }
        if (isNull(entityMap.get(focusIds.get(focusIds.size() - 1)))) {
            focusIds.removeIf(e -> isNull(entityMap.get(e)));
            return getFocus();
        }
        return entityMap.get(focusIds.get(focusIds.size() - 1));
    }

    public void addFocusId(int entityId) {
        focusIds.removeIf(e -> e.equals(entityId));
        focusIds.add(entityId);
    }

    public void clearFocusIds() {
        focusIds.clear();
    }

    @Subscribe
    public void handleNewPlayerInVisualRangeEvent(NewPlayerInVisualRangeEvent event) {
        addFocusId(event.playerEntity.getEntityId());
    }

}