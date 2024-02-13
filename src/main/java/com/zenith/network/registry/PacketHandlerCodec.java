package com.zenith.network.registry;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.network.client.ClientSession;
import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.EnumMap;

import static com.zenith.Shared.CONFIG;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PacketHandlerCodec {
    private final EnumMap<ProtocolState, PacketHandlerStateCodec<? extends Session>> stateCodecs;
    @NonNull
    private final Logger logger;

    public static Builder builder() {
        return new Builder();
    }

    private static final PacketHandlerStateCodec defaultStateCodec = PacketHandlerStateCodec.builder().build();

    public <S extends Session> @NonNull PacketHandlerStateCodec<S> getCodec(ProtocolState state) {
        return this.stateCodecs.getOrDefault(state, defaultStateCodec);
    }

    public <P extends Packet, S extends Session> P handleInbound(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packetLog.enabled) {
            var isClient = session instanceof ClientSession;
            var packetLogConfig = isClient ? CONFIG.debug.packetLog.clientPacketLog : CONFIG.debug.packetLog.serverPacketLog;
            if (packetLogConfig.received)
                if (CONFIG.debug.packetLog.packetFilter.isEmpty() || packet.getClass().getSimpleName().toLowerCase().contains(CONFIG.debug.packetLog.packetFilter.toLowerCase()))
                    this.logger.debug("[{}] [{}] Received: {}", Instant.now().toEpochMilli(), session.getClass().getSimpleName(), packetLogConfig.receivedBody ? packet : packet.getClass());
        }
        return getCodec(session.getPacketProtocol().getState()).handleInbound(packet, session);
    }

    public <P extends Packet, S extends Session> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packetLog.enabled)  {
            var isClient = session instanceof ClientSession;
            var packetLogConfig = isClient ? CONFIG.debug.packetLog.clientPacketLog : CONFIG.debug.packetLog.serverPacketLog;
            if (packetLogConfig.preSent)
                if (CONFIG.debug.packetLog.packetFilter.isEmpty() || packet.getClass().getSimpleName().toLowerCase().contains(CONFIG.debug.packetLog.packetFilter.toLowerCase()))
                    this.logger.debug("[{}] [{}] Sending: {}", Instant.now().toEpochMilli(), session.getClass().getSimpleName(), packetLogConfig.preSentBody ? packet : packet.getClass());
        }
        return getCodec(session.getPacketProtocol().getState()).handleOutgoing(packet, session);
    }

    public <P extends Packet, S extends Session> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packetLog.enabled) {
            var isClient = session instanceof ClientSession;
            var packetLogConfig = isClient ? CONFIG.debug.packetLog.clientPacketLog : CONFIG.debug.packetLog.serverPacketLog;
            if (packetLogConfig.postSent)
                if (CONFIG.debug.packetLog.packetFilter.isEmpty() || packet.getClass().getSimpleName().toLowerCase().contains(CONFIG.debug.packetLog.packetFilter.toLowerCase()))
                    this.logger.debug("[{}] [{}] Sent: {}", Instant.now().toEpochMilli(), session.getClass().getSimpleName(), packetLogConfig.postSentBody ? packet : packet.getClass());
        }
        getCodec(session.getPacketProtocol().getState()).handlePostOutgoing(packet, session);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private final EnumMap<ProtocolState, PacketHandlerStateCodec<? extends Session>> aStateCodecs = new EnumMap<>(ProtocolState.class);
        @NonNull
        private Logger logger;

        public Builder state(ProtocolState state, PacketHandlerStateCodec<? extends Session> codec) {
            this.aStateCodecs.put(state, codec);
            return this;
        }

        public PacketHandlerCodec build() {
            return new PacketHandlerCodec(this.aStateCodecs, logger);
        }
    }
}
