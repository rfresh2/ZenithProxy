package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.discord.Embed;
import com.zenith.event.client.ClientDisconnectEvent;
import com.zenith.event.client.ClientTickEvent;
import com.zenith.event.player.PlayerConnectionRemovedEvent;
import com.zenith.event.player.PlayerLoginEvent;
import com.zenith.event.player.SpectatorConnectedEvent;
import com.zenith.event.player.SpectatorDisconnectedEvent;
import com.zenith.feature.coordobf.CoordOffset;
import com.zenith.feature.coordobf.ObfPlayerState;
import com.zenith.feature.coordobf.ServerTeleport;
import com.zenith.feature.coordobf.handlers.inbound.*;
import com.zenith.feature.coordobf.handlers.outbound.*;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockOffsetType;
import com.zenith.mc.block.BlockPos;
import com.zenith.mc.block.LocalizedCollisionBox;
import com.zenith.mc.dimension.DimensionData;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.network.codec.PacketCodecRegistries;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import com.zenith.network.codec.PacketLogPacketHandlerCodec;
import com.zenith.network.server.ServerSession;
import com.zenith.util.Wait;
import com.zenith.util.config.Config;
import com.zenith.util.config.Config.Client.Extra.CoordObfuscation.ObfuscationMode;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetCursorItemPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundSetPlayerInventoryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemFromBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class CoordObfuscation extends Module {
    private final Random random = new SecureRandom();
    private final Map<ServerSession, ObfPlayerState> playerStateMap = new ConcurrentHashMap<>();
    private final Cache<Integer, ServerTeleport> preTeleportPositionCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(Duration.ofSeconds(5L))
        .build();
    // todo: maybe add spectator entities to the main entity cache
    @Getter private final IntSet spectatorEntityIds = new IntOpenHashSet();

    private final Config.Debug.PacketLog.PacketLogConfig packetLogConfig = new Config.Debug.PacketLog.PacketLogConfig();
    private final PacketHandlerCodec beforeOffsetPacketLogger = new PacketLogPacketHandlerCodec(
        Integer.MAX_VALUE-2,
        "coord-obf-before-offset",
        s -> CONFIG.debug.packetLog.enabled && CONFIG.client.extra.coordObfuscation.debugPacketLog,
        ComponentLogger.logger("Module.CoordObf"),
        () -> packetLogConfig
    );
    public final String genericDisconnectReason = "bye";

    public CoordObfuscation() {
        packetLogConfig.preSent = true;
        packetLogConfig.preSentBody = true;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(PlayerConnectionRemovedEvent.class, this::onServerConnectionRemoved),
            of(PlayerLoginEvent.Pre.class, this::onPlayerLoginEvent),
            of(SpectatorConnectedEvent.class, this::onSpectatorConnected),
            of(SpectatorDisconnectedEvent.class, this::onSpectatorDisconnected),
            of(ClientTickEvent.class, this::checkNearBlockOffsets),
            of(ClientDisconnectEvent.class, this::onDisconnect)
        );
    }

    private void onDisconnect(ClientDisconnectEvent event) {
        preTeleportPositionCache.invalidateAll();
    }

    private void onSpectatorDisconnected(SpectatorDisconnectedEvent event) {
        spectatorEntityIds.remove(event.serverSession().getSpectatorEntityId());
    }

    private void onSpectatorConnected(SpectatorConnectedEvent event) {
        spectatorEntityIds.add(event.session().getSpectatorEntityId());
    }

    private void checkNearBlockOffsets(ClientTickEvent event) {
        if (!CONFIG.client.extra.coordObfuscation.disconnectWhileNearOffsetBlocks) return;
        if (playerStateMap.isEmpty()) return;
        if (!isNearOffsetBlockStates()) return;
        for (var session : playerStateMap.keySet()) {
            disconnect(session, "Near illegal blocks", "Nearby collidable blocks with position offsets");
        }
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.coordObfuscation.enabled;
    }

    public boolean shouldObfuscateSession(ServerSession session) {
        return playerStateMap.containsKey(session);
    }

    @Override
    public PacketHandlerCodec registerServerPacketHandlerCodec() {
        return PacketHandlerCodec.serverBuilder()
            .setId("coord-obf")
            .setPriority(Integer.MAX_VALUE-1) // 1 less than packet logger
            .setActivePredicate(this::shouldObfuscateSession)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundAcceptTeleportationPacket.class, new COAcceptTeleportationHandler())
                .inbound(ServerboundMoveVehiclePacket.class, new COSMoveVehicleHandler())
                .inbound(ServerboundPlayerActionPacket.class, new COPlayerActionHandler())
                .inbound(ServerboundMovePlayerPosPacket.class, new COMovePlayerPosHandler())
                .inbound(ServerboundMovePlayerPosRotPacket.class, new COMovePlayerPosRotHandler())
                .inbound(ServerboundSignUpdatePacket.class, new COSignUpdateHandler())
                .inbound(ServerboundUseItemPacket.class, new COUseItemHandler())
                .inbound(ServerboundUseItemOnPacket.class, new COUseItemOnHandler())
                .inbound(ServerboundPickItemFromBlockPacket.class, new COPickItemFromBlockHandler())
                .outbound(ClientboundStartConfigurationPacket.class, new COCStartConfigurationHandler())
                .outbound(ClientboundAddEntityPacket.class, new COAddEntityHandler())
                .outbound(ClientboundAddExperienceOrbPacket.class, new COAddExperienceOrbHandler())
                .outbound(ClientboundBlockDestructionPacket.class, new COBlockDestructionHandler())
                .outbound(ClientboundBlockEntityDataPacket.class, new COBlockEntityDataHandler())
                .outbound(ClientboundBlockEventPacket.class, new COBlockEventHandler())
                .outbound(ClientboundBlockUpdatePacket.class, new COBlockUpdateHandler())
                .outbound(ClientboundChunksBiomesPacket.class, new COChunksBiomesHandler())
                .outbound(ClientboundContainerSetContentPacket.class, new COContainerSetContentHandler())
                .outbound(ClientboundContainerSetSlotPacket.class, new COContainerSetSlotHandler())
                .outbound(ClientboundDamageEventPacket.class, new CODamageEventHandler())
                .outbound(ClientboundExplodePacket.class, new COExplodeHandler())
                .outbound(ClientboundForgetLevelChunkPacket.class, new COForgetLevelChunkHandler())
                .outbound(ClientboundLevelChunkWithLightPacket.class, new COLevelChunkWithLightHandler())
                .outbound(ClientboundLevelEventPacket.class, new COLevelEventHandler())
                .outbound(ClientboundLevelParticlesPacket.class, new COLevelParticlesHandler())
                .outbound(ClientboundLightUpdatePacket.class, new COLightUpdateHandler())
                .outbound(ClientboundLoginPacket.class, new COLoginHandler())
                .outbound(ClientboundMoveEntityPosPacket.class, new COMoveEntityPosHandler())
                .outbound(ClientboundMoveEntityPosRotPacket.class, new COMoveEntityPosRotHandler())
                .outbound(ClientboundMoveEntityRotPacket.class, new COMoveEntityRotHandler())
                .outbound(ClientboundMoveVehiclePacket.class, new COCMoveVehicleHandler())
                .outbound(ClientboundOpenSignEditorPacket.class, new COOpenSignEditorHandler())
                .outbound(ClientboundPlayerLookAtPacket.class, new COPlayerLookAtHandler())
                .outbound(ClientboundPlayerPositionPacket.class, new COPlayerPositionHandler())
                .outbound(ClientboundRespawnPacket.class, new CORespawnHandler())
                .outbound(ClientboundSectionBlocksUpdatePacket.class, new COSectionBlocksUpdateHandler())
                .outbound(ClientboundSetChunkCacheCenterPacket.class, new COSetChunkCacheCenterHandler())
                .outbound(ClientboundSetDefaultSpawnPositionPacket.class, new COSetDefaultSpawnPositionHandler())
                .outbound(ClientboundSetEntityDataPacket.class, new COSetEntityDataHandler())
                .outbound(ClientboundSetEntityMotionPacket.class, new COSetEntityMotionHandler())
                .outbound(ClientboundSetEquipmentPacket.class, new COSetEquipmentHandler())
                .outbound(ClientboundSoundPacket.class, new COSoundHandler())
                .outbound(ClientboundTagQueryPacket.class, new COTagQueryHandler())
                .outbound(ClientboundTeleportEntityPacket.class, new COTeleportEntityHandler())
                .outbound(ClientboundEntityPositionSyncPacket.class, new COEntityPositionSyncHandler())
                .outbound(ClientboundMoveMinecartPacket.class, new COMoveMinecartHandler())
                .outbound(ClientboundSetCursorItemPacket.class, new COSetCursorItemHandler())
                .outbound(ClientboundSetPlayerInventoryPacket.class, new COSetPlayerInventoryHandler())
                .build())
            .build();
    }

    @Override
    public PacketHandlerCodec registerClientPacketHandlerCodec() {
        return PacketHandlerCodec.clientBuilder()
            .setId("coord-obf-client")
            .setPriority(Integer.MAX_VALUE-1)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                .inbound(ClientboundPlayerPositionPacket.class, (packet, session) -> {
                    // by the time our server handler for teleports is called, we may have already updated the player cache pos
                    // subject to race conditions, as cache update is done on the client event loop
                    PlayerCache cache = CACHE.getPlayerCache();
                    preTeleportPositionCache.put(packet.getId(), new ServerTeleport(cache.getX(), cache.getY(), cache.getZ(), packet.getId()));
                    return packet;
                })
                .build())
            .build();
    }
    final AtomicLong awaitLoginsUntil = new AtomicLong(0);

    @Override
    public void onEnable() {
        reconnectAllActiveConnections("Module enabled");
        PacketCodecRegistries.SERVER_REGISTRY.register(beforeOffsetPacketLogger);
    }

    @Override
    public void onDisable() {
        reconnectAllActiveConnections("Module disabled");
        playerStateMap.clear();
        preTeleportPositionCache.invalidateAll();
        PacketCodecRegistries.SERVER_REGISTRY.unregister(beforeOffsetPacketLogger);
    }

    public ObfPlayerState getPlayerState(ServerSession session) {
        var state = playerStateMap.get(session);
        if (state == null) {
            if (!session.isDisconnected()) {
                error("Tried to get player state for {} but it was null", session.getName(), new Exception(""));
                disconnect(session, "Invalid state", "Tried to get player state but it was null");
            }
            return new ObfPlayerState(session);
        }
        return state;
    }

    public CoordOffset getCoordOffset(ServerSession session) {
        return getPlayerState(session).getCoordOffset();
    }

    public void onPlayerLoginEvent(final PlayerLoginEvent.Pre event) {
        ServerSession session = event.session();
        try {
            if(!Wait.waitUntil(() -> awaitLoginsUntil.get() < System.currentTimeMillis() || session.isDisconnected(), 5)) {
                disconnect(session, "Login took too long");
                return;
            }
            if (session.isDisconnected()) return;
            if (!Proxy.getInstance().isConnected()) {
                disconnect(session, "Disconnected");
                return;
            }
            var client = Proxy.getInstance().getClient();
            if (client == null) {
                disconnect(session, "Disconnected");
                return;
            }
            client.getClientEventLoop().submit(() -> {
                var profile = session.getProfileCache().getProfile();
                var proxyProfile = CACHE.getProfileCache().getProfile();
                if (CONFIG.client.extra.coordObfuscation.exemptProxyAccount && profile != null && proxyProfile != null && profile.getId().equals(proxyProfile.getId())) {
                    info("Exempted proxy account session with no offset: {}", profile.getName());
                    return;
                }
                if (client.isInQueue() || !client.isOnline()) {
                    disconnect(session, "Queueing");
                    return;
                }
                if (CACHE.getPlayerCache().isRespawning()) {
                    reconnect(session, "Respawn in progress");
                    return;
                }
                if (CACHE.getChunkCache().getCache().size() < 12) {
                    reconnect(session, "Chunk cache not populated");
                    return;
                }
                if (!CACHE.getPlayerCache().getTeleportQueue().isEmpty()) {
                    reconnect(session, "Teleport queue not empty");
                    return;
                }
                if (CONFIG.client.extra.coordObfuscation.disconnectWhileEyeOfEnderPresent && isEnderEyeInWorld()) {
                    disconnect(session, genericDisconnectReason, "An eye of ender is in the world");
                    return;
                }
                if (CONFIG.client.extra.coordObfuscation.disconnectWhileNearOffsetBlocks && isNearOffsetBlockStates()) {
                    disconnect(session, genericDisconnectReason, "Nearby collidable blocks with position offsets");
                    return;
                }
                var state = new ObfPlayerState(session);
                playerStateMap.put(session, state);
                var playerPos = state.getPlayerPos();
                var coordOffset = generateOffset(session, playerPos.getX(), playerPos.getZ());
                state.setCoordOffset(coordOffset);
                info("Offset for {}: {}, {}", profile.getName(), coordOffset.x(), coordOffset.z());
            }).get(10L, TimeUnit.SECONDS);
        } catch (final Exception e) {
            error("Failed to generate coord offset", e);
            disconnect(session, genericDisconnectReason, "Failed to generate coord offset");
        }
    }

    public void onServerConnectionRemoved(final PlayerConnectionRemovedEvent event) {
        playerStateMap.remove(event.serverConnection());
    }

    public void onConfigChange() {
        reconnectAllActiveConnections("Config changed");
    }

    private CoordOffset generateOffset(ServerSession session, double playerX, double playerZ) {
        return switch (CONFIG.client.extra.coordObfuscation.mode) {
            case ObfuscationMode.RANDOM_OFFSET -> generateRandomOffset(playerX, playerZ);
            case ObfuscationMode.CONSTANT_OFFSET -> generateConstantOffset(session, playerX, playerZ);
            case ObfuscationMode.AT_LOCATION -> generateLocationOffset(playerX, playerZ);
        };
    }

    private CoordOffset generateRandomOffset(final double playerX, final double playerZ) {
        int tries = 0;
        while (true) {
            if (tries++ > 100) {
                throw new RuntimeException("Failed to generate coord offset after 100 tries lol");
            }
            int x = generateRandomPos();
            int z = generateRandomPos();
            if (MathHelper.distance2d(playerX, playerZ, x, z) < CONFIG.client.extra.coordObfuscation.randomMinDistanceFromSelf)
                continue; // retry
            int xOffset = (x / 16) - MathHelper.floorI(playerX / 16);
            int zOffset = (z / 16) - MathHelper.floorI(playerZ / 16);
            debug("Generated random pos: {} {}", x, z);
            return new CoordOffset(xOffset, zOffset);
        }
    }

    private CoordOffset generateConstantOffset(final ServerSession session, final double playerX, final double playerZ) {
        if (MathHelper.distance2d(0, 0, playerX, playerZ) < CONFIG.client.extra.coordObfuscation.constantOffsetMinSpawnDistance) {
            disconnect(session, genericDisconnectReason, "Too close to spawn to use constant offset");
            return new CoordOffset(random.nextInt(12345, 999999), random.nextInt(12345, 999999));
        }

        if (CONFIG.client.extra.coordObfuscation.constantOffsetNetherTranslate) {
            DimensionData dimension = World.getCurrentDimension();
            if (dimension == DimensionRegistry.THE_NETHER.get()) {
                return new CoordOffset((CONFIG.client.extra.coordObfuscation.constantOffsetX / 16) / 8, (CONFIG.client.extra.coordObfuscation.constantOffsetZ / 16) / 8);
            }
        }
        return new CoordOffset(CONFIG.client.extra.coordObfuscation.constantOffsetX / 16, CONFIG.client.extra.coordObfuscation.constantOffsetZ / 16);
    }

    private CoordOffset generateLocationOffset(final double playerX, final double playerZ) {
        int playerChunkX = MathHelper.floorI(playerX / 16);
        int playerChunkZ = MathHelper.floorI(playerZ / 16);
        int xOffset = (CONFIG.client.extra.coordObfuscation.atLocationX / 16) - playerChunkX;
        int zOffset = (CONFIG.client.extra.coordObfuscation.atLocationZ / 16) - playerChunkZ;
        return new CoordOffset(xOffset, zOffset);
    }

    private int generateRandomPos() {
        int min = CONFIG.client.extra.coordObfuscation.randomMinDistanceFromSpawn;
        int max = CONFIG.client.extra.coordObfuscation.randomMaxDistanceFromSpawn;
        return random.nextInt(min, max) * (random.nextBoolean() ? 1 : -1);
    }

    public void playerMovePos(final ServerSession session, final double x, final double z) {
        var state = getPlayerState(session);
        MutableVec3d pos = state.getPlayerPos();
        if (!session.isSpectator()) {
            double playerMoveDist = MathHelper.distance2d(x, z, pos.getX(), pos.getZ());
            if (playerMoveDist > CONFIG.client.extra.coordObfuscation.teleportOffsetRegenerateDistanceMin) {
                info("Reconnecting {} due to long distance movement", session.getName());
                var reason = "Long distance movement attempted";
                var coordsPart = String.format(": %.1f blocks, from: [%.1f, %.1f] (player pos) to [%.1f, %.1f]",
                    playerMoveDist, pos.getX(), pos.getZ(), x, z);
                if (CONFIG.discord.reportCoords) {
                    // as is notified in discord
                    reason += coordsPart;
                } else {
                    // just to log
                    var reasonWithCoords = reason + coordsPart;
                    warn(reasonWithCoords);
                }
                reconnect(session, reason);
                return;
            }
            var playerMoveDist2 = MathHelper.distance2d(x, z, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getZ());
            if (playerMoveDist2 > CONFIG.client.extra.coordObfuscation.teleportOffsetRegenerateDistanceMin) {
                info("Reconnecting {} due to long distance movement", session.getName());
                var reason = "Long distance movement attempted";
                var coordsPart = String.format(": %.1f blocks, from: [%.1f, %.1f] (cached pos) to [%.1f, %.1f]",
                    playerMoveDist2, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getZ(), x, z);
                if (CONFIG.discord.reportCoords) {
                    // as is notified in discord
                    reason += coordsPart;
                } else {
                    // just to log
                    var reasonWithCoords = reason + coordsPart;
                    warn(reasonWithCoords);
                }
                reconnect(session, reason);
                return;
            }
        }
        pos.setX(x);
        pos.setZ(z);
    }

    public void setServerTeleportPos(final ServerSession session, final double x, final double y, final double z, final int teleportId) {
        if (session.isSpectator()) return; // ignore for spectators
        getPlayerState(session).getServerTeleports().add(new ServerTeleport(x, y, z, teleportId));
    }

    public void onServerTeleport(final ServerSession session, double x, double y, double z, final int teleportId, final List<PositionElement> relative) {
        // todo: find any cases where zenith is sending the teleport after the session is spawned
        if (teleportId == session.getSpawnTeleportId() && !session.isSpawned()) return;
        // spectator position resync see SpectatorSync$syncSpectatorPositionToEntity
        if (teleportId == session.getSpawnTeleportId() && session.isSpectator()) return;
        var tp = preTeleportPositionCache.getIfPresent(teleportId);
        if (tp == null) {
            warn("Unexpected teleport id {} for {}, known teleports: {}",
                teleportId,
                session.getName(),
                preTeleportPositionCache.asMap().keySet().stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"))
            );
            reconnect(session, "Unexpected teleport id");
            return;
        }
        if (relative.contains(PositionElement.X)) {
            x += tp.x();
        }
        if (relative.contains(PositionElement.Y)) {
            y += tp.y();
        }
        if (relative.contains(PositionElement.Z)) {
            z += tp.z();
        }
        setServerTeleportPos(session, x, y, z, teleportId);
        if (getPlayerState(session).isRespawning()) {
            reconnect(session, "Teleport during respawn");
//            var futureOffset = generateOffset(session, x, z);
//            info("Regenerated offset due to respawn teleport: {} {}", futureOffset.x(), futureOffset.z());
//            getPlayerState(session).setCoordOffset(futureOffset);
//            session.setRespawning(false);
            return;
        }

        if (MathHelper.distance2d(x, z, tp.x(), tp.z()) >= CONFIG.client.extra.coordObfuscation.teleportOffsetRegenerateDistanceMin) {
            info("Reconnecting {} due to long distance teleport using preTeleportPos calc", session.getName());
            var reason = "Long distance server teleport";
            var coordsPart = String.format(", from: [%.1f, %.1f] to: [%.1f, %.1f] (2)", tp.x(), tp.z(), x, z);
            if (CONFIG.discord.reportCoords) {
                // as is notified in discord
                reason += coordsPart;
            } else {
                // just to log
                var reasonWithCoords = reason + coordsPart;
                warn(reasonWithCoords);
                reason += " (2)";
            }
            reconnect(session, reason);
            return;
        }
        // it is possible for controlling players to manipulate the player cache position temporarily
        // but chunk cache should be not be able to be manipulated
        if (MathHelper.distance2d(x / 16, z / 16, CACHE.getChunkCache().getCenterX(), CACHE.getChunkCache().getCenterZ()) >= CONFIG.client.extra.coordObfuscation.teleportOffsetRegenerateDistanceMin / 16.0) {
            var reason = "Long distance server teleport";
            var coordsPart = String.format(", from: [%.1f, %.1f] to: [%.1f, %.1f] (1)\", tp.x(), tp.z(), x, z)", tp.x(), tp.z(), x, z);
            if (CONFIG.discord.reportCoords) {
                // as is notified in discord
                reason += coordsPart;
            } else {
                // just to log
                var reasonWithCoords = reason + coordsPart;
                warn(reasonWithCoords);
                reason += " (1)";
            }
            reconnect(session, reason);
            return;
        }
    }

    public void onRespawn(final ServerSession session, final int dimension) {
        getPlayerState(session).setRespawning(true);
        // todo: could be possible to avoid a reconnect here
        //  needs more testing
        reconnect(session, "Respawning");
    }

    public void reconnect(ServerSession session, String reason) {
        var embed = Embed.builder()
            .title("Reconnecting Player")
            .addField("Player", "[" + session.getName() + "](https://namemc.com/profile/" + session.getUUID() + ")")
            .addField("Reason", reason)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(session.getUUID()).toString())
            .errorColor();
        discordNotification(embed);
        delayIncomingLogins();
        if (session.isSpectator()) {
            session.transferToSpectator();
        } else {
            session.transferToControllingPlayer();
        }
    }

    private void reconnectAllActiveConnections(String reason) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var con = connections[i];
            reconnect(con, reason);
        }
    }

    public void disconnect(ServerSession session, String publicReason, String privateReason) {
        if (session.isDisconnected()) return; // already disconnected
        disconnectNotif(session, privateReason);
        delayIncomingLogins();
        super.disconnect(session, publicReason);
    }

    @Override
    public void disconnect(ServerSession session, String minimessage) {
        if (session.isDisconnected()) return; // already disconnected
        disconnectNotif(session, minimessage);
        delayIncomingLogins();
        super.disconnect(session, minimessage);
    }

    private void disconnectNotif(final ServerSession session, final String reason) {
        var embed = Embed.builder()
            .title("Disconnecting Player")
            .addField("Player", "[" + session.getName() + "](https://namemc.com/profile/" + session.getUUID() + ")")
            .addField("Reason", reason)
            .thumbnail(Proxy.getInstance().getPlayerBodyURL(session.getUUID()).toString())
            .errorColor();
        discordNotification(embed);
    }

    public void delayIncomingLogins() {
        awaitLoginsUntil.set(System.currentTimeMillis() + CONFIG.client.extra.coordObfuscation.delayPlayerLoginsAfterTpMs);
    }

    public boolean isEnderEyeInWorld() {
        for (var entity : CACHE.getEntityCache().getEntities().values()) {
            if (entity.getEntityType() == EntityType.EYE_OF_ENDER) return true;
            if (entity.getEntityType() == EntityType.ITEM) {
                var itemStack = entity.getMetadataValue(8, MetadataTypes.ITEM, ItemStack.class);
                if (itemStack == null) continue;
                if (itemStack.getId() == ItemRegistry.ENDER_EYE.id()) return true;
            }
        }
        return false;
    }

    public boolean isNearOffsetBlockStates() {
        var playerCb = new LocalizedCollisionBox(BOT.getCollisionBox(Pose.STANDING), CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ());
        for (var posLong : World.getBlockPosLongListInCollisionBox(playerCb)) {
            int blockStateId = World.getBlockStateId(BlockPos.getX(posLong), BlockPos.getY(posLong), BlockPos.getZ(posLong));
            var block = World.getBlock(blockStateId);
            if (block.offsetType() == BlockOffsetType.NONE) continue;
            var blockCb = BLOCK_DATA.getCollisionBoxesFromBlockStateId(blockStateId);
            if (blockCb.isEmpty()) continue;
            return true;
        }
        return false;
    }

    public record ValidationResult(boolean valid, List<String> invalidReasons) {}

    public ValidationResult validateSetup() {
        // check all modules and settings that could lead to leaking coordinates or the offset

        boolean valid = true;
        List<String> invalidReasons = new ArrayList<>();

        // ingame commands, both sending/receiving
        if (CONFIG.inGameCommands.enable) {
            invalidReasons.add("In-game commands should be disabled, many commands leak coordinates in outputs and behavior: `commandConfig ingame off`");
            valid = false;
        }
        if (!CONFIG.client.extra.actionLimiter.enabled) {
            invalidReasons.add("ActionLimiter should be enabled: `actionLimiter on`");
            valid = false;
        }
        if (CONFIG.client.extra.actionLimiter.allowMovement) {
            invalidReasons.add("ActionLimiter movement should be disabled to prevent long distance movement: `actionLimiter allowMovement off`");
            valid = false;
        }
        if (CONFIG.client.extra.actionLimiter.allowRespawn) {
            invalidReasons.add("Action Limiter respawns should be disabled to prevent respawning: `actionLimiter allowRespawn off`");
            valid = false;
        }
        if (CONFIG.client.extra.actionLimiter.allowChat) {
            invalidReasons.add("Action Limiter chat should be disabled to prevent you getting muted or AntiLeak interactions: `actionLimiter allowChat off`");
            valid = false;
        }
        if (CONFIG.client.extra.actionLimiter.allowServerCommands) {
            invalidReasons.add("Action Limiter server commands should be disabled to prevent `/kill` or whispers: `actionLimiter allowServerCommands off`");
            valid = false;
        }

        return new ValidationResult(valid, invalidReasons);
    }
}
