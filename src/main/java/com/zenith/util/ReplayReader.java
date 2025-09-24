package com.zenith.util;

import com.zenith.feature.replay.ReplayMetadata;
import com.zenith.mc.biome.BiomeRegistry;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.network.client.ClientSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.PalettedWorldState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.zenith.Globals.BLOCK_DATA;
import static com.zenith.Globals.GSON;

public class ReplayReader {
    private final File mcprFile;
    private final File packetLogOutputFile;
    private static final ByteBufAllocator ALLOC = ByteBufAllocator.DEFAULT;

    public ReplayReader(File replayFile, File packetLogOutputFile) {
        this.mcprFile = replayFile;
        this.packetLogOutputFile = packetLogOutputFile;
    }

    @SneakyThrows
    public void read() {
        // validate file exists
        if (!mcprFile.exists()) throw new RuntimeException("Replay file does not exist");

        // validate file ends in .mcpr
        if (!mcprFile.getName().endsWith(".mcpr")) throw new RuntimeException("Replay file does not have .mcpr extension");

        // validate file is a valid zip file
        try (var zip = new ZipFile(mcprFile)) {
            // validate zip file contains metaData.json
            ZipEntry metadataEntry = zip.getEntry("metaData.json");
            if (metadataEntry == null) throw new RuntimeException("Replay file does not contain metaData.json");
            try (var metadataEntryStream = zip.getInputStream(metadataEntry)) {
                try (Reader reader = new InputStreamReader(metadataEntryStream)) {
                    ReplayMetadata metadata = GSON.fromJson(reader, ReplayMetadata.class);
                    // validate metaData.json mcversion matches our version
                    if (!metadata.getMcversion().equals(MinecraftCodec.CODEC.getMinecraftVersion()))
                        throw new RuntimeException("Replay file mcversion does not match current version. Expected: " + MinecraftCodec.CODEC.getMinecraftVersion() + " Actual: " + metadata.getMcversion());
                }
            }
            // validate zip file contains recording.tmcpr
            ZipEntry recordingEntry = zip.getEntry("recording.tmcpr");
            if (recordingEntry == null) throw new RuntimeException("Replay file does not contain recording.tmcpr");
            try (var recordingEntryStream = zip.getInputStream(recordingEntry)) {
                try (var recordingStream = new DataInputStream(new BufferedInputStream(recordingEntryStream))) {
                    read(recordingStream);
                }
            }
        }
    }

    @SneakyThrows
    private void read(final DataInputStream recordingStream) {
        MinecraftProtocol packetProtocol = new MinecraftProtocol();
        packetProtocol.setTargetState(ProtocolState.GAME);
        packetProtocol.setUseDefaultListeners(false);
        packetProtocol.setInboundState(ProtocolState.LOGIN);
        var session = new ClientSession("", 0, "", packetProtocol, null);
        try (var outputWriter = new BufferedOutputStream(new FileOutputStream(packetLogOutputFile))) {
            while (recordingStream.available() > 0) {
                readRecordingEntry(recordingStream, packetProtocol, session, outputWriter);
            }
        }
    }

    @SneakyThrows
    private void readRecordingEntry(final DataInputStream recordingStream, final MinecraftProtocol packetProtocol, final ClientSession session, final BufferedOutputStream outputWriter) {
        int t = recordingStream.readInt();
        int len = recordingStream.readInt();
        ByteBuf byteBuf = ALLOC.buffer();
        try {
            byteBuf.writeBytes(recordingStream, len);
            int packetId = packetProtocol.getPacketHeader().readPacketId(byteBuf);
            Packet packet = packetProtocol.getInboundPacketRegistry().createClientboundPacket(packetId, byteBuf, session);
            String out = "\n[" + t + "] " + packet.toString();
            outputWriter.write(out.getBytes(StandardCharsets.UTF_8));
            switch (packetProtocol.getInboundState()) {
                case ProtocolState.LOGIN -> {
                    if (packet instanceof ClientboundLoginFinishedPacket) {
                        packetProtocol.setInboundState(ProtocolState.CONFIGURATION);
                    }
                }
                case ProtocolState.CONFIGURATION -> {
                    if (packet instanceof ClientboundFinishConfigurationPacket) {
                        packetProtocol.setInboundState(ProtocolState.GAME);
                    }
                }
                case ProtocolState.GAME -> {
                    if (packet instanceof ClientboundStartConfigurationPacket) {
                        packetProtocol.setInboundState(ProtocolState.CONFIGURATION);
                    }
                }
            }
            if (packet instanceof ClientboundLoginPacket loginPacket) {
                session.setPalettedWorldState(new PalettedWorldState(
                    DimensionRegistry.REGISTRY.get(loginPacket.getCommonPlayerSpawnInfo().getDimension()).sectionCount(),
                    BLOCK_DATA.blockStateRegistrySize(),
                    BlockRegistry.AIR.minStateId(),
                    BiomeRegistry.REGISTRY.size(),
                    BiomeRegistry.PLAINS.get().id()
                ));
            } else if (packet instanceof ClientboundRespawnPacket respawnPacket) {
                session.setPalettedWorldState(new PalettedWorldState(
                    DimensionRegistry.REGISTRY.get(respawnPacket.getCommonPlayerSpawnInfo().getDimension()).sectionCount(),
                    BLOCK_DATA.blockStateRegistrySize(),
                    BlockRegistry.AIR.minStateId(),
                    BiomeRegistry.REGISTRY.size(),
                    BiomeRegistry.PLAINS.get().id()
                ));
            }
        } catch (final Throwable e) {
            outputWriter.write("\nError reading recording entry".getBytes(StandardCharsets.UTF_8));
            throw e;
        } finally {
            byteBuf.release();
        }
    }
}
