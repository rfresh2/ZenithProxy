/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.util.handler;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.toobeetooteebot.util.Constants;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HandlerRegistry<S extends Session> {
    @NonNull
    private final Map<Class<? extends Packet>, BiConsumer<? extends Packet, S>> inboundHandlers;

    @NonNull
    private final Map<Class<? extends Packet>, BiFunction<? extends Packet, S, ? extends Packet>> outboundHandlers;

    @SuppressWarnings("unchecked")
    public <P extends Packet> void handleInbound(@NonNull P packet, @NonNull S session) {
        BiConsumer<P, S> handler = (BiConsumer<P, S>) this.inboundHandlers.get(packet.getClass());
        if (handler != null)    {
            handler.accept(packet, session);
        }
    }

    @SuppressWarnings("unchecked")
    public <P extends Packet> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        BiFunction<P, S, P> handler = (BiFunction<P, S, P>) this.outboundHandlers.get(packet.getClass());
        return handler == null ? packet : handler.apply(packet, session);
    }

    public static class Builder<S extends Session>   {
        private final Map<Class<? extends Packet>, BiConsumer<? extends Packet, S>> inboundHandlers = new IdentityHashMap<>();

        private final Map<Class<? extends Packet>, BiFunction<? extends Packet, S, ? extends Packet>> outboundHandlers = new IdentityHashMap<>();

        public <P extends Packet> Builder<S> registerInbound(@NonNull Class<P> clazz, @NonNull BiConsumer<P, S> handler)    {
            this.inboundHandlers.put(clazz, handler);
            return this;
        }

        public Builder<S> registerInbound(@NonNull IncomingHandler<? extends Packet, S> handler)    {
            this.inboundHandlers.put(handler.getPacketClass(), handler);
            return this;
        }

        public <P extends Packet> Builder<S> registerOutbound(@NonNull Class<P> clazz, @NonNull BiFunction<P, S, P> handler)    {
            this.outboundHandlers.put(clazz, handler);
            return this;
        }

        public Builder<S> registerOutbound(@NonNull OutgoingHandler<? extends Packet, S> handler)    {
            this.outboundHandlers.put(handler.getPacketClass(), handler);
            return this;
        }

        public HandlerRegistry<S> build()   {
            return new HandlerRegistry<>(this.inboundHandlers, this.outboundHandlers);
        }
    }

    public interface IncomingHandler<P extends Packet, S extends Session> extends BiConsumer<P, S>, Constants {
        @Override
        void accept(P packet, S session);

        Class<P> getPacketClass();
    }

    public interface OutgoingHandler<P extends Packet, S extends Session> extends BiFunction<P, S, P>, Constants  {
        @Override
        P apply(P packet, S session);

        Class<P> getPacketClass();
    }
}
