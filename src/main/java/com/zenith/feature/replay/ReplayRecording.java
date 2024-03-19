package com.zenith.feature.replay;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerSpawnInfo;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.packetlib.Session;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.Proxy;
import com.zenith.feature.spectator.SpectatorPacketProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
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
    private OutputStream outputStream;
    private ZipOutputStream zipOutputStream;
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
        metadata.setDate(Instant.now().toEpochMilli());
        metadata.setMcversion(MinecraftCodec.CODEC.getMinecraftVersion());
        metadata.setProtocol(MinecraftCodec.CODEC.getProtocolVersion());
        // todo: when to init?
//        metadata.setSelfId(CACHE.getPlayerCache().getEntityId());
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        final String time = formatter.format(ZonedDateTime.now());
        replayFile = replayDirectory.resolve(time + "_recording.mcpr").toFile();
        replayFile.getParentFile().mkdirs();
        outputStream = new BufferedOutputStream(new FileOutputStream(replayFile));
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.putNextEntry(new ZipEntry("recording.tmcpr"));
        if (Proxy.getInstance().isConnected() && Proxy.getInstance().getClient().isOnline()) {
            lateStartRecording();
        } else {
            preConnectRecording();
        }
    }

    // Start recording while we already have a logged in session
    private void lateStartRecording() {
        writePacket0(0, new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile()), Proxy.getInstance().getClient(), ProtocolState.LOGIN);
        CACHE.getConfigurationCache().getPackets(packet -> writePacket0(Instant.now().toEpochMilli(), (MinecraftPacket) packet, Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION));
        writePacket0(Instant.now().toEpochMilli(), new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
        writePacket0(Instant.now().toEpochMilli(), new ClientboundFinishConfigurationPacket(), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
        writePacket(Instant.now().toEpochMilli(), new ClientboundLoginPacket(
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
                CACHE.getChunkCache().getCurrentDimension().dimensionName(),
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
            .forEach(d -> d.getPackets(packet -> writePacket(Instant.now().toEpochMilli(), (MinecraftPacket) packet, Proxy.getInstance().getClient())));
        SpectatorPacketProvider.playerSpawn().forEach(p -> writePacket(Instant.now().toEpochMilli(), (MinecraftPacket) p, Proxy.getInstance().getClient()));
        SpectatorPacketProvider.playerPosition().forEach(p -> writePacket(Instant.now().toEpochMilli(), (MinecraftPacket) p, Proxy.getInstance().getClient()));
    }

    // Start recording before we've connected
    // need to wait for login packets
    private void preConnectRecording() {
        preConnectSyncNeeded = true;
    }

    public void writePacket(final long time, final MinecraftPacket packet, final Session session) {
        var protocolState = session.getPacketProtocol().getState();
        if (protocolState != ProtocolState.GAME) return;
        executor.execute(() -> writePacket0(time, packet, session, protocolState));
    }

    private void writePacket0(final long time, final MinecraftPacket packet, final Session session, final ProtocolState protocolState) {
        try {
            writeToFile(time, packet, session, protocolState);
        } catch (final Throwable e) {
            MODULE_LOG.error("Failed to write packet {}", packet.getClass().getSimpleName(), e);
        }
    }

    private void writeToFile(final long time, final MinecraftPacket packet, final Session session, final ProtocolState protocolState) {
        int t = (int) time;
        if (t == 0) {
            startT = Instant.now().toEpochMilli();
        } else {
            t = (int) (time - startT);
        }
        final ByteBuf packetBuf = ALLOC.buffer();
        try {
            var packetProtocol = session.getPacketProtocol();
            var codecHelper = (MinecraftCodecHelper) session.getCodecHelper();
            var packetId = MinecraftCodec.CODEC.getCodec(protocolState).getClientboundId(packet);
            packetProtocol.getPacketHeader().writePacketId(packetBuf, codecHelper, packetId);
            packet.serialize(packetBuf, codecHelper);
            var packetSize = packetBuf.readableBytes();
            writeInt(zipOutputStream, t);
            writeInt(zipOutputStream, packetSize);
            packetBuf.readBytes(zipOutputStream, packetSize);
        } catch (final Throwable e) {
            MODULE_LOG.error("Failed to write packet {}", packet.getClass().getSimpleName(), e);
        } finally {
            packetBuf.release();
        }
    }

    /**
     * Writes an integer to the output stream.
     * @param out The output stream
     * @param x The integer
     * @throws IOException if an I/O error occurs.
     */
    public static void writeInt(OutputStream out, int x) throws IOException {
        out.write((x >>> 24) & 0xFF);
        out.write((x >>> 16) & 0xFF);
        out.write((x >>>  8) & 0xFF);
        out.write(x & 0xFF);
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
                MODULE_LOG.error("Failed waiting for termination of ReplayMod PacketHandler executor", e);
            }
        }
        if (zipOutputStream != null) {
            zipOutputStream.closeEntry();
            metadata.setDuration((int) (System.currentTimeMillis() - startT));
            zipOutputStream.putNextEntry(new ZipEntry("metaData.json"));
            zipOutputStream.write(GSON.toJson(metadata).getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.close();
        }
        if (outputStream != null) {
            outputStream.close();
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
            // send mutated entity metadata
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
                CACHE.getConfigurationCache().getPackets(packet2 -> writeToFile(Instant.now().toEpochMilli(), (MinecraftPacket) packet2, Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION));
                writeToFile(Instant.now().toEpochMilli(), new ClientboundCustomPayloadPacket("minecraft:brand", CACHE.getChunkCache().getServerBrand()), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
                writeToFile(Instant.now().toEpochMilli(), new ClientboundFinishConfigurationPacket(), Proxy.getInstance().getClient(), ProtocolState.CONFIGURATION);
                time = Instant.now().toEpochMilli();
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
