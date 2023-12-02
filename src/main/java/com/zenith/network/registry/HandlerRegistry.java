package com.zenith.network.registry;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.network.server.handler.shared.incoming.KeepAliveHandler;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.zenith.Shared.CONFIG;


@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HandlerRegistry<S extends Session> {
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers;
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers;
    @NonNull
    protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers;
    @NonNull
    protected final Logger logger;
    protected final boolean allowUnhandled;
    protected static final ExecutorService ASYNC_EXECUTOR_SERVICE =
        Executors.newFixedThreadPool(1,
                                     new ThreadFactoryBuilder()
                                       .setNameFormat("ZenithProxy Async PacketHandler #%d")
                                       .setDaemon(true)
                                       .build());
    protected static final ScheduledExecutorService RETRY_EXECUTOR_SERVICE =
        Executors.newScheduledThreadPool(1,
                                         new ThreadFactoryBuilder()
                                             .setNameFormat("ZenithProxy Async PacketHandler Retry #%d")
                                             .setDaemon(true)
                                             .build());

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleInbound(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.received)  {
            if (!(packet instanceof ClientboundKeepAlivePacket || packet instanceof KeepAliveHandler)) {
                this.logger.debug("[{}] Received: {}", Instant.now().toEpochMilli(), CONFIG.debug.packet.receivedBody ? packet : packet.getClass());
            }
//            if (allowedPackets.stream().anyMatch(allowPacket -> packet.getClass() == allowPacket)) {
//                this.logger.debug("Received packet: {}@%08x", CONFIG.debug.packet.receivedBody ? packet : packet.getClass(), System.identityHashCode(packet));
//            }
        }
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.inboundHandlers.get(packet.getClass());
        if (handler == null) {
            if (allowUnhandled) return packet;
            else return null;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.preSent)  {
            this.logger.debug("[{}] Sending: {}", Instant.now().toEpochMilli(), CONFIG.debug.packet.preSentBody ? packet : packet.getClass());
        }
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
        if (CONFIG.debug.packet.postSent) {
            if (!(packet instanceof ClientboundKeepAlivePacket || packet instanceof KeepAliveHandler))
                this.logger.debug("[{}] Sent: {}", Instant.now().toEpochMilli(), CONFIG.debug.packet.postSentBody ? packet : packet.getClass());
//            if (allowedPackets.stream().anyMatch(allowPacket -> packet.getClass() == allowPacket)) {
//                this.logger.debug("Sent packet: {}@%08x", CONFIG.debug.packet.postSentBody ? packet : packet.getClass(), System.identityHashCode(packet));
//            }
        }
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.postOutboundHandlers.get(packet.getClass());
        if (handler != null) {
            handler.apply(packet, session);
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder<S extends Session> {

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers = new Reference2ObjectOpenHashMap<>();

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> outboundHandlers = new Reference2ObjectOpenHashMap<>();

        protected final Reference2ObjectMap<Class<? extends Packet>, PacketHandler<? extends Packet, S>> postOutboundHandlers = new Reference2ObjectOpenHashMap<>();
        @NonNull
        protected Logger logger;
        protected boolean allowUnhandled = true;

        public Builder<S> registerInbound(@NonNull Class<? extends Packet> packetClass, @NonNull PacketHandler<? extends Packet, S> handler) {
            this.inboundHandlers.put(packetClass, handler);
            return this;
        }

        public Builder<S> registerOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull PacketHandler<? extends Packet, S> handler) {
            this.outboundHandlers.put(packetClass, handler);
            return this;
        }

        public Builder<S> registerPostOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull PostOutgoingPacketHandler<? extends Packet, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public Builder<S> registerPostOutbound(@NonNull Class<? extends Packet> packetClass, @NonNull AsyncPacketHandler<? extends Packet, S> handler) {
            this.postOutboundHandlers.put(packetClass, handler);
            return this;
        }

        public Builder<S> allowUnhandled(final boolean allowUnhandled) {
            this.allowUnhandled = allowUnhandled;
            return this;
        }

        public HandlerRegistry<S> build() {
            return new HandlerRegistry<>(this.inboundHandlers, this.outboundHandlers, this.postOutboundHandlers, this.logger, this.allowUnhandled);
        }
    }
}
