/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.util.handler;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.util.PacketHandler;
import com.zenith.util.Wait;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.lib.logging.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.zenith.util.Constants.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author DaPorkchop_
 */
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

    protected static final ExecutorService ASYNC_EXECUTOR_SERVICE = Executors.newFixedThreadPool(10); // idk 10 seems reasonable but might need to adjust

    @SuppressWarnings("unchecked")
    public <P extends Packet> boolean handleInbound(@NonNull P packet, @NonNull S session) {
        if (CONFIG.debug.packet.received)  {
            this.logger.debug("Received packet: %s@%08x", CONFIG.debug.packet.receivedBody ? packet : packet.getClass(), System.identityHashCode(packet));
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
            this.logger.debug("Sending packet: %s@%08x", CONFIG.debug.packet.preSentBody ? packet : packet.getClass(), System.identityHashCode(packet));
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
            this.logger.debug("Sent packet: %s@%08x", CONFIG.debug.packet.postSentBody ? packet : packet.getClass(), System.identityHashCode(packet));
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
                        Wait.waitALittleMs(50);
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

        protected final Map<Class<? extends Packet>, PacketHandler<? extends Packet, S>> inboundHandlers = new IdentityHashMap<>();

        protected final Map<Class<? extends Packet>, BiFunction<? extends Packet, S, ? extends Packet>> outboundHandlers = new IdentityHashMap<>();

        protected final Map<Class<? extends Packet>, BiConsumer<? extends Packet, S>> postOutboundHandlers = new IdentityHashMap<>();

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
