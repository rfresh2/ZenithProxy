package com.zenith.util.handler;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.util.PacketHandler;
import com.zenith.util.Wait;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.zenith.util.Constants.CLIENT_LOG;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HandlerRegistry<S extends Session> {
    @NonNull
    protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers;

    @NonNull
    protected final Map<Class<? extends Packet>, BiFunction<? extends Packet, S, ? extends Packet>> outboundHandlers;

    @NonNull
    protected final Map<Class<? extends Packet>, BiConsumer<? extends Packet, S>> postOutboundHandlers;

    @NonNull
    protected final Logger logger;

    protected final boolean allowUnhandled;

    protected static final ExecutorService ASYNC_EXECUTOR_SERVICE = Executors.newFixedThreadPool(8);
    private static final List<Class<? extends Packet>> allowedPackets = asList(
            ServerPlayerPositionRotationPacket.class,
            ClientPlayerPositionRotationPacket.class,
            ClientPlayerRotationPacket.class,
            ClientPlayerPositionPacket.class,
            ClientTeleportConfirmPacket.class);

    @SuppressWarnings("unchecked")
    public <P extends Packet> boolean handleInbound(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.received)  {
            if (allowedPackets.stream().anyMatch(allowPacket -> packet.getClass() == allowPacket)) {
                this.logger.debug("Received packet: {}@%08x", CONFIG.debug.packet.receivedBody ? packet : packet.getClass(), System.identityHashCode(packet));
            }
        }
        PacketHandler<P, S> handler = (PacketHandler<P, S>) this.inboundHandlers.get(packet.getClass());
        if (isNull(handler)) {
            return allowUnhandled;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.preSent)  {
            this.logger.debug("Sending packet: {}@%08x", CONFIG.debug.packet.preSentBody ? packet : packet.getClass(), System.identityHashCode(packet));
        }
        BiFunction<P, S, P> handler = (BiFunction<P, S, P>) this.outboundHandlers.get(packet.getClass());
        if (isNull(handler)) {
            // allowUnhandled has no effect here
            return packet;
        } else {
            return handler.apply(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.postSent)  {
            if (allowedPackets.stream().anyMatch(allowPacket -> packet.getClass() == allowPacket)) {
                this.logger.debug("Sent packet: {}@%08x", CONFIG.debug.packet.postSentBody ? packet : packet.getClass(), System.identityHashCode(packet));
            }
        }
        PostOutgoingHandler<P, S> handler = (PostOutgoingHandler<P, S>) this.postOutboundHandlers.get(packet.getClass());
        if (nonNull(handler)) {
            handler.accept(packet, session);
        }
    }

    public interface IncomingHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {
        /**
         * Handle a packet
         *
         * @param packet  the packet to handle
         * @param session the session the packet was received on
         * @return whether or not the packet should be forwarded
         */
        boolean apply(P packet, S session);

        Class<P> getPacketClass();
    }

    public interface AsyncIncomingHandler<P extends Packet, S extends Session> extends PacketHandler<P, S> {

        /**
         * Call async (non-cancellable)
         * @param packet packet to handle
         * @param session Session the packet was received on
         */
        @Override
        default boolean apply(P packet, S session) {
            ASYNC_EXECUTOR_SERVICE.submit(() -> {
                try {
                    int iterCount = 0;
                    while (!applyAsync(packet, session)) {
                        Wait.waitALittleMs(200);
                        if (iterCount++ > 3) {
                            CLIENT_LOG.warn("Unable to apply async handler for packet: " + packet.getClass().getSimpleName());
                            break;
                        }
                    }
                } catch (final Throwable e) {
                    CLIENT_LOG.error("Async handler error", e);
                }
            });
            return true;
        }

        boolean applyAsync(P packet, S session);

        Class<P> getPacketClass();
    }

    public interface OutgoingHandler<P extends Packet, S extends Session> extends BiFunction<P, S, P> {
        @Override
        P apply(P packet, S session);

        Class<P> getPacketClass();
    }

    public interface PostOutgoingHandler<P extends Packet, S extends Session> extends BiConsumer<P, S> {
        @Override
        void accept(P packet, S session);

        Class<P> getPacketClass();
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder<S extends Session> {

        protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers = new Object2ObjectOpenHashMap<>();

        protected final Map<Class<? extends Packet>, BiFunction<? extends Packet, S, ? extends Packet>> outboundHandlers = new Object2ObjectOpenHashMap<>();

        protected final Map<Class<? extends Packet>, BiConsumer<? extends Packet, S>> postOutboundHandlers = new Object2ObjectOpenHashMap<>();

        @NonNull
        protected Logger logger;

        protected boolean allowUnhandled = true;

        public <P extends Packet> Builder<S> registerInbound(@NonNull Class<P> clazz, @NonNull BiConsumer<P, S> handler) {
            return this.registerInbound(clazz, (packet, session) -> {
                handler.accept(packet, session);
                return true;
            });
        }

        public <P extends Packet> Builder<S> registerInbound(@NonNull Class<P> clazz, @NonNull PacketHandler<P, S> handler) {
            this.inboundHandlers.put(clazz, handler);
            return this;
        }

        public Builder<S> registerInbound(@NonNull IncomingHandler<? extends Packet, S> handler) {
            this.inboundHandlers.put(handler.getPacketClass(), handler);
            return this;
        }

        public Builder<S> registerInbound(@NonNull AsyncIncomingHandler<? extends Packet, S> handler) {
            this.inboundHandlers.put(handler.getPacketClass(), handler);
            return this;
        }

        public <P extends Packet> Builder<S> registerOutbound(@NonNull Class<P> clazz, @NonNull BiFunction<P, S, P> handler) {
            this.outboundHandlers.put(clazz, handler);
            return this;
        }

        public Builder<S> registerOutbound(@NonNull OutgoingHandler<? extends Packet, S> handler) {
            this.outboundHandlers.put(handler.getPacketClass(), handler);
            return this;
        }

        public <P extends Packet> Builder<S> registerPostOutbound(@NonNull Class<P> clazz, @NonNull BiConsumer<P, S> handler) {
            this.postOutboundHandlers.put(clazz, handler);
            return this;
        }

        public Builder<S> registerPostOutbound(@NonNull PostOutgoingHandler<? extends Packet, S> handler) {
            this.postOutboundHandlers.put(handler.getPacketClass(), handler);
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
