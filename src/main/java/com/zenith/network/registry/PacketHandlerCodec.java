package com.zenith.network.registry;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Predicate;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PacketHandlerCodec {
    private final int priority;
    @EqualsAndHashCode.Include private final String id;
    private final EnumMap<ProtocolState, PacketHandlerStateCodec<? extends Session>> stateCodecs;
    // Predicate called on each packet to determine if it should be handled by this codec
    private final Predicate<Session> activePredicate;

    public static Builder builder() {
        return new Builder();
    }

    protected static final PacketHandlerStateCodec defaultStateCodec = PacketHandlerStateCodec.builder().build();

    public <S extends Session> @NonNull PacketHandlerStateCodec<S> getCodec(ProtocolState state) {
        return this.stateCodecs.getOrDefault(state, defaultStateCodec);
    }

    public <P extends Packet, S extends Session> P handleInbound(@NonNull P packet, @NonNull S session) {
        return getCodec(session.getPacketProtocol().getState()).handleInbound(packet, session);
    }

    public <P extends Packet, S extends Session> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        return getCodec(session.getPacketProtocol().getState()).handleOutgoing(packet, session);
    }

    public <P extends Packet, S extends Session> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        getCodec(session.getPacketProtocol().getState()).handlePostOutgoing(packet, session);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder {
        private final EnumMap<ProtocolState, PacketHandlerStateCodec<? extends Session>> aStateCodecs = new EnumMap<>(ProtocolState.class);
        private int priority;
        private String id;
        private Predicate<Session> activePredicate = session -> true;

        public Builder state(ProtocolState state, PacketHandlerStateCodec<? extends Session> codec) {
            this.aStateCodecs.put(state, codec);
            return this;
        }

        public PacketHandlerCodec build() {
            Objects.requireNonNull(this.id, "id");
            return new PacketHandlerCodec(priority, id, this.aStateCodecs, activePredicate);
        }
    }
}
