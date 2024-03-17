package com.zenith.feature.replay;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.*;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.packetlib.Session;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

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
    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;
    private boolean loginPhase = true;
    private long startT;
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
        final File file = replayDirectory.resolve(time + "_recording.mcpr").toFile();
        file.getParentFile().mkdirs();
        outputStream = new BufferedOutputStream(new FileOutputStream(file));
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.putNextEntry(new ZipEntry("recording.tmcpr"));
        startT = System.currentTimeMillis();
    }

    public void writePacket(final long time, final MinecraftPacket packet, final Session session) {
        var protocolState = session.getPacketProtocol().getState();
        if (protocolState != ProtocolState.GAME) return;
        executor.execute(() -> writePacket0(time, packet, session, protocolState));
    }

    private void writePacket0(final long time, final MinecraftPacket packet, final Session session, final ProtocolState protocolState) {
        try {
            if (loginPhase) {
                writeToFile(0, new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile()), session, ProtocolState.LOGIN);
            }
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
        if (packet instanceof ClientboundGameProfilePacket) {
            loginPhase = false;
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

    public void translateOutgoingPacket(final long time, final MinecraftPacket packet, final Session session) {
        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            if (recordSelfSpawn) {
                recordSelfSpawn = !recordSelfSpawn;
                var spawnPacket = new ClientboundAddPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getThePlayer().getUuid(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch());
                var entityMetadataPacket = new ClientboundSetEntityDataPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()
                );
                writePacket(time, spawnPacket, session);
                writePacket(time, entityMetadataPacket, session);
            }
        } else if (packet instanceof ServerboundMovePlayerPosPacket
            || packet instanceof ServerboundMovePlayerPosRotPacket
            || packet instanceof ServerboundMovePlayerRotPacket) {
            var teleportEntityPacket = new ClientboundTeleportEntityPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                CACHE.getPlayerCache().getYaw(),
                CACHE.getPlayerCache().getPitch(),
                false
            );
            var rotateHeadPacket = new ClientboundRotateHeadPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().getYaw()
            );
            writePacket(time, teleportEntityPacket, session);
            writePacket(time, rotateHeadPacket, session);
        } else if (packet instanceof ServerboundContainerClickPacket
            || packet instanceof ServerboundContainerClosePacket
            || packet instanceof ServerboundPlayerActionPacket) {
            var helmet = new Equipment(EquipmentSlot.HELMET, CACHE.getPlayerCache().getEquipment(EquipmentSlot.HELMET));
            var chestplate = new Equipment(EquipmentSlot.CHESTPLATE, CACHE.getPlayerCache().getEquipment(EquipmentSlot.CHESTPLATE));
            var leggings = new Equipment(EquipmentSlot.LEGGINGS, CACHE.getPlayerCache().getEquipment(EquipmentSlot.LEGGINGS));
            var boots = new Equipment(EquipmentSlot.BOOTS, CACHE.getPlayerCache().getEquipment(EquipmentSlot.BOOTS));
            var mainHand = new Equipment(EquipmentSlot.MAIN_HAND, CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND));
            var offHand = new Equipment(EquipmentSlot.OFF_HAND, CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND));
            var equipmentPacket = new ClientboundSetEquipmentPacket(
                CACHE.getPlayerCache().getEntityId(),
                new Equipment[] { helmet, chestplate, leggings, boots, mainHand, offHand });
            writePacket(time, equipmentPacket, session);
        } else if (packet instanceof ServerboundSwingPacket) {
            var swingPacket = new ClientboundAnimatePacket(
                CACHE.getPlayerCache().getEntityId(),
                Animation.SWING_ARM
            );
            writePacket(time, swingPacket, session);
        } else if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
            // send mutated entity metadata
        }
    }

    public void handleInboundPacket(final long time, final MinecraftPacket packet, final Session session) {
        if (packet instanceof ClientboundLoginPacket loginPacket) {
            recordSelfSpawn = true;
        }
        writePacket(time, packet, session);
    }
}
