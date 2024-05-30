package com.zenith.feature.replay;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorPacketProvider;
import com.zenith.module.impl.ReplayMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Getter;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;

import java.io.*;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.zenith.Shared.*;

public class ReplayRecording implements Closeable {
    private final ReplayMetadata metadata;
    private final Path replayDirectory;
    private OutputStream fileOutputStream;
    private ZipOutputStream zipOutputStream;
    private OutputStream writerStream;
    @Getter private File replayFile;
    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;
    private boolean preConnectSyncNeeded = false;
    @Getter private long startT;
    private final ExecutorService executor = Executors.newFixedThreadPool(
        1,
        new ThreadFactoryBuilder()
            .setNameFormat("ZenithProxy ReplayMod PacketHandler #%d")
            .setDaemon(true)
            .build());

    public ReplayRecording(final Path replayDirectory) {
        this.metadata = new ReplayMetadata();
        this.replayDirectory = replayDirectory;
    }

    public void startRecording() throws Exception{
        // initialize output streams and metadata
        var serverName = CONFIG.client.server.address;
        if (CONFIG.client.server.port != 25565)
            serverName += ":" + CONFIG.client.server.port;
        metadata.setServerName(serverName);
        metadata.setDate(System.currentTimeMillis());
        metadata.setMcversion(MinecraftCodec.CODEC.getMinecraftVersion());
        metadata.setProtocol(MinecraftCodec.CODEC.getProtocolVersion());
        // todo: when to init?
//        metadata.setSelfId(CACHE.getPlayerCache().getEntityId());
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        final String time = formatter.format(ZonedDateTime.now());
        replayFile = replayDirectory.resolve(time + "_" + CONFIG.authentication.username + ".mcpr").toFile();
        replayFile.getParentFile().mkdirs();
        fileOutputStream = new FileOutputStream(replayFile);
        zipOutputStream = new ZipOutputStream(fileOutputStream);
        zipOutputStream.putNextEntry(new ZipEntry("recording.tmcpr"));
        writerStream = new BufferedOutputStream(zipOutputStream);
        if (Proxy.getInstance().isConnected() && Proxy.getInstance().getClient().isOnline()) {
            lateStartRecording();
        } else {
            preConnectRecording();
        }
    }

    // Start recording while we already have a logged in session
    private void lateStartRecording() {
        writePacket0(0, new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile()), Proxy.getInstance().getClient(), ProtocolState.LOGIN);
        CACHE.getConfigurationCache().getPackets(packet -> writePacket0(System.currentTimeMillis(), (MinecraftPacket) packet, Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION));
        writePacket0(System.currentTimeMillis(), new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
        writePacket0(System.currentTimeMillis(), new ClientboundFinishConfigurationPacket(), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
        writePacket(System.currentTimeMillis(), new ClientboundLoginPacket(
            CACHE.getPlayerCache().getEntityId(),
            CACHE.getPlayerCache().isHardcore(),
            CACHE.getChunkCache().getDimensionRegistry().keySet().toArray(new String[0]),
            CACHE.getPlayerCache().getMaxPlayers(),
            CACHE.getChunkCache().getServerViewDistance(),
            CACHE.getChunkCache().getServerSimulationDistance(),
            CACHE.getPlayerCache().isReducedDebugInfo(),
            CACHE.getPlayerCache().isEnableRespawnScreen(),
            CACHE.getPlayerCache().isDoLimitedCrafting(),
            new PlayerSpawnInfo(
                CACHE.getChunkCache().getCurrentDimension().name(),
                CACHE.getChunkCache().getWorldName(),
                CACHE.getChunkCache().getHashedSeed(),
                CACHE.getPlayerCache().getGameMode(),
                CACHE.getPlayerCache().getGameMode(),
                CACHE.getChunkCache().isDebug(),
                CACHE.getChunkCache().isFlat(),
                CACHE.getPlayerCache().getLastDeathPos(),
                CACHE.getPlayerCache().getPortalCooldown()
            )
        ), Proxy.getInstance().getClient());
        CACHE.getAllData()
            .forEach(d -> d.getPackets(packet -> writePacket(System.currentTimeMillis(), (MinecraftPacket) packet, Proxy.getInstance().getClient())));
        SpectatorPacketProvider.playerSpawn().forEach(p -> writePacket(System.currentTimeMillis(), (MinecraftPacket) p, Proxy.getInstance().getClient()));
        SpectatorPacketProvider.playerPosition().forEach(p -> writePacket(System.currentTimeMillis(), (MinecraftPacket) p, Proxy.getInstance().getClient()));
        SpectatorPacketProvider.playerEquipment().forEach(p -> writePacket(System.currentTimeMillis(), (MinecraftPacket) p, Proxy.getInstance().getClient()));
    }

    // Start recording before we've connected
    // need to wait for login packets
    private void preConnectRecording() {
        preConnectSyncNeeded = true;
    }

    public void writePacket(final long time, final MinecraftPacket packet, final Session session) {
        var protocolState = session.getPacketProtocol().getState();
        if (protocolState != ProtocolState.GAME) return;
        if (!executor.isShutdown()) executor.execute(() -> writePacket0(time, packet, session, protocolState));
    }

    private void writePacket0(final long time, final MinecraftPacket packet, final Session session, final ProtocolState protocolState) {
        try {
            writeToFile(time, packet, session, protocolState);
        } catch (final Throwable e) {
            MODULE.get(ReplayMod.class).error("Failed to write packet {}", packet.getClass().getSimpleName(), e);
        }
    }

    private void writeToFile(final long time, final MinecraftPacket packet, final Session session, final ProtocolState protocolState) {
        int t = (int) time;
        if (t == 0) {
            startT = System.currentTimeMillis();
        } else {
            t = (int) (time - startT);
        }
        final ByteBuf packetBuf = ALLOC.heapBuffer();
        try {
            packetBuf.writeInt(t);
            var lenIndex = packetBuf.writerIndex();
            packetBuf.writeInt(0); // write dummy length
            var packetProtocol = session.getPacketProtocol();
            var codecHelper = (MinecraftCodecHelper) session.getCodecHelper();
            var packetId = MinecraftCodec.CODEC.getCodec(protocolState).getClientboundId(packet);
            packetProtocol.getPacketHeader().writePacketId(packetBuf, codecHelper, packetId);
            packet.serialize(packetBuf, codecHelper);
            var packetSize = packetBuf.readableBytes();
            var packetBodySize = packetSize - 8;
            packetBuf.setInt(lenIndex, packetBodySize); // write actual length
            packetBuf.readBytes(writerStream, packetSize);
        } catch (final Throwable e) {
            MODULE.get(ReplayMod.class).error("Failed to write packet {}", packet.getClass().getSimpleName(), e);
        } finally {
            packetBuf.release();
        }
    }

    @Override
    public void close() throws IOException {
        if (!executor.isShutdown()) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new Exception("");
                }
            } catch (final Exception e) {
                MODULE.get(ReplayMod.class).error("Failed waiting for termination of ReplayMod PacketHandler executor", e);
            }
        }
        if (writerStream != null) {
            writerStream.flush();
            zipOutputStream.closeEntry();
            metadata.setDuration((int) (System.currentTimeMillis() - startT));
            zipOutputStream.putNextEntry(new ZipEntry("metaData.json"));
            zipOutputStream.write(GSON.toJson(metadata).getBytes());
            zipOutputStream.closeEntry();
            writerStream.close();
            zipOutputStream.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    private boolean recordSelfSpawn = false;

    public void handleOutgoingPacket(final long time, final MinecraftPacket packet, final Session session) {
        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            if (recordSelfSpawn) {
                recordSelfSpawn = false;
                SpectatorPacketProvider.playerSpawn().forEach(p -> writePacket(time, (MinecraftPacket) p, session));
            }
        } else if (packet instanceof ServerboundMovePlayerPosPacket
            || packet instanceof ServerboundMovePlayerPosRotPacket
            || packet instanceof ServerboundMovePlayerRotPacket) {
            SpectatorPacketProvider.playerPosition().forEach(p -> writePacket(time, (MinecraftPacket) p, session));
        } else if (packet instanceof ServerboundContainerClickPacket
            || packet instanceof ServerboundContainerClosePacket
            || packet instanceof ServerboundPlayerActionPacket) {
            SpectatorPacketProvider.playerEquipment().forEach(p -> writePacket(time, (MinecraftPacket) p, session));
        } else if (packet instanceof ServerboundSwingPacket) {
            SpectatorPacketProvider.playerSwing().forEach(p -> writePacket(time, (MinecraftPacket) p, session));
        } else if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
            SpectatorPacketProvider.playerSneak().forEach(p -> writePacket(time, (MinecraftPacket) p, session));
        }
        /**
         * Known issues because we don't cache these states:
         *
         * Block breaking progress
         * Sleeping animation
         */
    }

    public void handleInboundPacket(long time, final MinecraftPacket packet, final Session session) {
        if (packet instanceof ClientboundLoginPacket loginPacket) {
            recordSelfSpawn = true;
            if (preConnectSyncNeeded) {
                writeToFile(0, new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile()), session, ProtocolState.LOGIN);
                CACHE.getConfigurationCache().getPackets(packet2 -> writeToFile(System.currentTimeMillis(), (MinecraftPacket) packet2, Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION));
                writeToFile(System.currentTimeMillis(), new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
                writeToFile(System.currentTimeMillis(), new ClientboundFinishConfigurationPacket(), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
                time = System.currentTimeMillis();
                preConnectSyncNeeded = false;
            }
        }
        writePacket(time, packet, session);
        if (packet instanceof ClientboundRespawnPacket respawnPacket) {
            final long t = time;
            SpectatorPacketProvider.playerSpawn().forEach(p -> writePacket(t, (MinecraftPacket) p, session));
        }
    }
}
