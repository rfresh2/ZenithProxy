package com.zenith.feature.spectator;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRotateHeadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.feature.spectator.entity.mob.SpectatorEntityEnderDragon;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.math.MathHelper;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

public final class SpectatorUtils {

    public static void syncPlayerEquipmentWithSpectatorsFromCache() {
        sendSpectatorsEquipment();
    }

    public static void syncPlayerPositionWithSpectators() {
        Proxy.getInstance().getSpectatorConnections().forEach(connection -> {
            connection.send(new ClientboundTeleportEntityPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    false // idk if this will break any rendering or not
            ));
            connection.send(new ClientboundRotateHeadPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getYaw()
            ));
        });
    }

    public static void syncSpectatorPositionToEntity(final ServerConnection spectConnection, UUID target) {
        if (target != null) {
            boolean hasUpdatedPos = false;
            if (CACHE.getProfileCache().getProfile().getId().equals(target)) {
                spectConnection.getSpectatorPlayerCache()
                        .setX(CACHE.getPlayerCache().getX())
                        .setY(CACHE.getPlayerCache().getY() + 1) // spawn above entity
                        .setZ(CACHE.getPlayerCache().getZ())
                        .setYaw(CACHE.getPlayerCache().getYaw())
                        .setPitch(CACHE.getPlayerCache().getPitch());
                hasUpdatedPos = true;
            } else {
                for (Entity entity: CACHE.getEntityCache().getEntities().values()) {
                    if (entity.getUuid().equals(target)) {
                        spectConnection.getSpectatorPlayerCache()
                                .setX(entity.getX())
                                .setY(entity.getY() + 1) // spawn above entity
                                .setZ(entity.getZ())
                                .setYaw(entity.getYaw())
                                .setPitch(entity.getPitch());
                        hasUpdatedPos = true;
                        break;
                    }
                }
            }
            if (hasUpdatedPos) {
                spectConnection.setAllowSpectatorServerPlayerPosRotate(true);
                spectConnection.send(new ClientboundPlayerPositionPacket(
                        spectConnection.getSpectatorPlayerCache().getX(),
                        spectConnection.getSpectatorPlayerCache().getY(),
                        spectConnection.getSpectatorPlayerCache().getZ(),
                        spectConnection.getSpectatorPlayerCache().getYaw(),
                        spectConnection.getSpectatorPlayerCache().getPitch(),
                        12345
                ));
                spectConnection.setAllowSpectatorServerPlayerPosRotate(false);
                updateSpectatorPosition(spectConnection);

                Proxy.getInstance().getActiveConnections().forEach(c -> {
                    if (!c.equals(spectConnection) || spectConnection.isShowSelfEntity()) {
                        c.send(spectConnection.getEntitySpawnPacket());
                        c.send(spectConnection.getEntityMetadataPacket());
                    }
                });
            }
        }
    }
    public static void syncSpectatorPositionToProxiedPlayer(final ServerConnection spectConnection) {
        syncSpectatorPositionToEntity(spectConnection, CACHE.getProfileCache().getProfile().getId());
    }

    private static void sendSpectatorsEquipment() {
        Proxy.getInstance().getSpectatorConnections().forEach(SpectatorUtils::sendSpectatorsEquipment);
    }

    private static void sendSpectatorsEquipment(final ServerConnection connection) {
        connection.send(new ClientboundSetEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getThePlayer().getEquipment().entrySet().stream()
                    .map(entry -> new Equipment(entry.getKey(), entry.getValue()))
                    .toArray(Equipment[]::new)));
    }

    private static void spawnSpectatorForOtherSessions(ServerConnection session, ServerConnection connection) {
        if (connection.equals(session.getProxy().getCurrentPlayer().get())) {
            session.send(new ClientboundAddPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getProfileCache().getProfile().getId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch()));
            session.send(new ClientboundSetEntityDataPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
        } else {
            session.send(connection.getEntitySpawnPacket());
            session.send(connection.getEntityMetadataPacket());
        }
        connection.send(session.getEntitySpawnPacket());
        connection.send(session.getEntityMetadataPacket());
    }

    public static EntityPlayer getSpectatorPlayerEntity(final ServerConnection session) {
        EntityPlayer spectatorEntityPlayer = new EntityPlayer();
        spectatorEntityPlayer.setUuid(session.getProfileCache().getProfile().getId());
        spectatorEntityPlayer.setSelfPlayer(true);
        spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
        spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY() + 1); // spawn above player
        spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
        spectatorEntityPlayer.setEntityId(session.getSpectatorSelfEntityId());
        spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
        spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
        final CompoundTag emptyNbtTag = new CompoundTag("");
        emptyNbtTag.clear();
        spectatorEntityPlayer.setMetadata(asList(
            new FloatEntityMetadata(9, MetadataType.FLOAT, 20.0f), // health
            new ByteEntityMetadata(17, MetadataType.BYTE, (byte) 255) // visible skin parts
        ));
        return spectatorEntityPlayer;
    }

    public static void initSpectator(ServerConnection session, Supplier<Collection<CachedData>> cacheSupplier) {
        // update spectator player cache
        EntityPlayer spectatorEntityPlayer = getSpectatorPlayerEntity(session);
        session.getSpectatorPlayerCache()
            .setThePlayer(spectatorEntityPlayer)
            .setGameMode(SPECTATOR)
            .setEnableRespawnScreen(CACHE.getPlayerCache().isEnableRespawnScreen())
            .setLastDeathPos(CACHE.getPlayerCache().getLastDeathPos())
            .setPortalCooldown(CACHE.getPlayerCache().getPortalCooldown())
            .setHardcore(CACHE.getPlayerCache().isHardcore())
            .setReducedDebugInfo(CACHE.getPlayerCache().isReducedDebugInfo())
            .setHeldItemSlot(CACHE.getPlayerCache().getHeldItemSlot())
            .setEnabledFeatures(CACHE.getPlayerCache().getEnabledFeatures())
            .setDifficultyLocked(CACHE.getPlayerCache().isDifficultyLocked())
            .setInvincible(true)
            .setCanFly(true)
            .setCreative(false)
            .setFlying(true)
            .setFlySpeed(0.05f)
            .setWalkSpeed(0.1f)
            .setTags(CACHE.getPlayerCache().getTags())
            .setOpLevel(CACHE.getPlayerCache().getOpLevel())
            .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.setAllowSpectatorServerPlayerPosRotate(true);
        DataCache.sendCacheData(cacheSupplier.get(), session);
        session.setAllowSpectatorServerPlayerPosRotate(false);
        session.send(session.getEntitySpawnPacket());
        session.send(session.getSelfEntityMetadataPacket());
        session.getProxy().getActiveConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    spawnSpectatorForOtherSessions(session, connection);
                    connection.syncTeamMembers();
                });
        session.send(new ClientboundSetEntityDataPacket(session.getSpectatorSelfEntityId(), spectatorEntityPlayer.getEntityMetadataAsArray()));
        SpectatorUtils.syncPlayerEquipmentWithSpectatorsFromCache();
    }

    public static void checkSpectatorPositionOutOfRender(final ServerConnection spectConnection) {
        final int spectX = (int) spectConnection.getSpectatorPlayerCache().getX() >> 4;
        final int spectZ = (int) spectConnection.getSpectatorPlayerCache().getZ() >> 4;
        final int playerX = (int) CACHE.getPlayerCache().getX() >> 4;
        final int playerZ = (int) CACHE.getPlayerCache().getZ() >> 4;
        if (Math.abs(spectX - playerX) > (CACHE.getChunkCache().getRenderDistance() / 2 + 1) || Math.abs(spectZ - playerZ) > (CACHE.getChunkCache().getRenderDistance() / 2 + 1)) {
            SpectatorUtils.syncSpectatorPositionToProxiedPlayer(spectConnection);
        }
    }

    public static void updateSpectatorPosition(final ServerConnection selfSession) {
        if (selfSession.isPlayerCam()) {
            return;
        }
        double playerEyeHeight = 1.6;
        double specEntityEyeHeight = selfSession.getSpectatorEntity().getEyeHeight();
        double specEntityTotalWidth = selfSession.getSpectatorEntity().getWidth();
        // clamping avoids moving the spectator entity directly in view of the player in 1st person at steep pitches
        float clampedPitch = MathHelper.clamp(selfSession.getSpectatorPlayerCache().getPitch(), -30, 30);

        double distance = (specEntityTotalWidth * -0.5) - 0.5;
        double yawRadians = Math.toRadians(selfSession.getSpectatorPlayerCache().getYaw());
        double pitchRadians = Math.toRadians(clampedPitch);

        double xOffset = -Math.sin(yawRadians) * Math.cos(pitchRadians) * distance;
        double yOffset = -Math.sin(pitchRadians) * distance;
        double zOffset = Math.cos(yawRadians) * Math.cos(pitchRadians) * distance;

        double newX = selfSession.getSpectatorPlayerCache().getX() + xOffset;
        double newY = selfSession.getSpectatorPlayerCache().getY() + playerEyeHeight - specEntityEyeHeight + yOffset;
        double newZ = selfSession.getSpectatorPlayerCache().getZ() + zOffset;

        Proxy.getInstance().getActiveConnections()
                .forEach(connection -> {
                    connection.send(new ClientboundTeleportEntityPacket(
                        selfSession.getSpectatorEntityId(),
                        newX,
                        newY,
                        newZ,
                        getDisplayYaw(selfSession),
                        selfSession.getSpectatorPlayerCache().getPitch(),
                        false
                    ));
                    connection.send(new ClientboundRotateHeadPacket(
                        selfSession.getSpectatorEntityId(),
                        getDisplayYaw(selfSession)
                    ));
                });
    }

    public static float getDisplayYaw(final ServerConnection serverConnection) {
        // idk why but dragon is displayed 180 degrees off from what you'd expect
        if (serverConnection.getSpectatorEntity() instanceof SpectatorEntityEnderDragon) {
            return serverConnection.getSpectatorPlayerCache().getYaw() - 180f;
        } else {
            return serverConnection.getSpectatorPlayerCache().getYaw();
        }
    }
}
