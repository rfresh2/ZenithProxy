package com.zenith.network.codec;

import com.zenith.network.client.ClientSession;
import com.zenith.network.server.ServerSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.jspecify.annotations.NonNull;

import java.util.IdentityHashMap;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PacketHandlerStateCodec<S extends Session> {
    @NonNull
    protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers;
    @NonNull
    protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers;
    @NonNull
    protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers;
    // If set to false, cancels any inbound packet type with no handler registered
    protected final boolean allowUnhandledInbound;

    public static <S extends Session> Builder<S> builder() {
        return new Builder<>();
    }

    public static Builder<ClientSession> clientBuilder() {
        return new Builder<>();
    }

    public static Builder<ServerSession> serverBuilder() {
        return new Builder<>();
    }

    public <P extends Packet> P handleInbound(@NonNull P packet, @NonNull S session) {
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.inboundHandlers.get(packet.getClass());
        if (handler == null) {
            if (allowUnhandledInbound) return packet;
            else return null;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.outboundHandlers.get(packet.getClass());
        if (handler == null) {
            // allowUnhandled has no effect here
            return packet;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.postOutboundHandlers.get(packet.getClass());
        if (handler != null) {
            handler.apply(packet, session);
        }
    }

    public static class Builder<S extends Session> {

        protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers = new IdentityHashMap<>();

        protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers = new IdentityHashMap<>();

        protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers = new IdentityHashMap<>();
        // If set to false, cancels any inbound packet type with no handler registered
        protected boolean allowUnhandledInbound = true;

        public <P extends Packet> PacketHandlerStateCodec.Builder<S> inbound(@NonNull Class<P> packetClass, @NonNull PacketHandler<P, S> handler) {
            this.inboundHandlers.put(packetClass, handler);
            return this;
        }

        public <P extends Packet> PacketHandlerStateCodec.Builder<S> outbound(@NonNull Class<P> packetClass, @NonNull PacketHandler<P, S> handler) {
            this.outboundHandlers.put(packetClass, handler);
            return this;
        }

        public <P extends Packet> PacketHandlerStateCodec.Builder<S> postOutbound(@NonNull Class<P> packetClass, @NonNull PostOutgoingPacketHandler<P, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public <P extends Packet> PacketHandlerStateCodec.Builder<S> postOutbound(@NonNull Class<P> packetClass, @NonNull AsyncPacketHandler<P, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public <P extends Packet> PacketHandlerStateCodec.Builder<S> postOutbound(@NonNull Class<P> packetClass, @NonNull ClientEventLoopPacketHandler<P, ClientSession> handler) {
            this.postOutboundHandlers.put(packetClass, (PacketHandler<P, S>) handler);
            return this;
        }

        // If set to false, cancels any inbound packet type with no handler registered
        public PacketHandlerStateCodec.Builder<S> allowUnhandledInbound(final boolean allowUnhandled) {
            this.allowUnhandledInbound = allowUnhandled;
            return this;
        }

        public PacketHandlerStateCodec<S> build() {
            return new PacketHandlerStateCodec<>(this.inboundHandlers, this.outboundHandlers, this.postOutboundHandlers, this.allowUnhandledInbound);
        }
    }
}
