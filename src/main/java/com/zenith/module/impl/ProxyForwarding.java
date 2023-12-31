package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.event.Subscription;
import com.zenith.feature.forwarding.handlers.inbound.ForwardingHandshakeHandler;
import com.zenith.feature.forwarding.handlers.inbound.ForwardingHelloHandler;
import com.zenith.feature.forwarding.handlers.inbound.ForwardingLoginQueryResponseHandler;
import com.zenith.feature.forwarding.handlers.outbound.ForwardingGameProfileHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.HandlerRegistry;
import com.zenith.network.server.ServerConnection;
import lombok.Getter;
import net.kyori.adventure.key.Key;

import java.util.function.Supplier;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_LOG;

public class ProxyForwarding extends Module {
    public static final byte VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION = (byte) 1;
    public static final Key VELOCITY_PLAYER_INFO_CHANNEL = Key.key("velocity", "player_info");
    public static final int VELOCITY_QUERY_ID = 1;

    @Getter
    private HandlerRegistry<ServerConnection> handlerRegistry;

    public ProxyForwarding() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        handlerRegistry = new HandlerRegistry.Builder<ServerConnection>()
                .setLogger(MODULE_LOG)
                .allowUnhandled(true)
                .registerInbound(ClientIntentionPacket.class, new ForwardingHandshakeHandler())
                .registerInbound(ServerboundHelloPacket.class, new ForwardingHelloHandler())
                .registerInbound(ServerboundCustomQueryPacket.class, new ForwardingLoginQueryResponseHandler())
                .registerOutbound(ClientboundGameProfilePacket.class, new ForwardingGameProfileHandler())
                .build();
    }

    @Override
    public Subscription subscribeEvents() {
        return null;
    }

    @Override
    public Supplier<Boolean> shouldBeEnabled() {
        return () -> CONFIG.client.extra.proxyForwarding.enabled;
    }
}
