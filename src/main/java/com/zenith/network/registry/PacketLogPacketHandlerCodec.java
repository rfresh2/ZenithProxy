package com.zenith.network.registry;

import com.zenith.util.Config.Debug.PacketLog.PacketLogConfig;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.EnumMap;
import java.util.function.Supplier;

import static com.zenith.Shared.CONFIG;

class PacketLogPacketHandlerCodec extends PacketHandlerCodec {
    // delaying instantiation so graalvm doesn't init this at build-time
    private final Supplier<PacketLogConfig> packetLogConfigSupplier;
    private PacketLogConfig packetLogConfig;
    private final Logger logger;
    private final LoggingEventBuilder log;

    public PacketLogPacketHandlerCodec(
        final String id,
        final Logger logger,
        Supplier<PacketLogConfig> packetLogConfigSupplier
    ) {
        super(Integer.MAX_VALUE, id, new EnumMap<>(ProtocolState.class), (session) -> CONFIG.debug.packetLog.enabled);
        this.logger = logger;
        this.log = logger.atLevel(CONFIG.debug.packetLog.logLevelDebug ? Level.DEBUG : Level.INFO);
        this.packetLogConfigSupplier = packetLogConfigSupplier;
    }

    private PacketLogConfig getPacketLogConfig() {
        if (packetLogConfig == null)
            packetLogConfig = packetLogConfigSupplier.get();
        return packetLogConfig;
    }

    @Override
    public <P extends Packet, S extends Session> P handleInbound(@NonNull P packet, @NonNull S session) {
        if (getPacketLogConfig().received && shouldLog(packet))
            log.log("[{}] [{}] Received: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), getPacketLogConfig().receivedBody ? packet : packet.getClass().getSimpleName());
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        if (getPacketLogConfig().preSent && shouldLog(packet))
            log.log("[{}] [{}] Sending: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), getPacketLogConfig().preSentBody ? packet : packet.getClass().getSimpleName());
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        if (getPacketLogConfig().postSent && shouldLog(packet))
            log.log("[{}] [{}] Sent: {}", System.currentTimeMillis(), session.getClass().getSimpleName(), getPacketLogConfig().postSentBody ? packet : packet.getClass().getSimpleName());
    }

    private boolean shouldLog(Packet packet) {
        return CONFIG.debug.packetLog.packetFilter.isEmpty()
            || packet.getClass().getSimpleName().toLowerCase().contains(CONFIG.debug.packetLog.packetFilter.toLowerCase());
    }
}
