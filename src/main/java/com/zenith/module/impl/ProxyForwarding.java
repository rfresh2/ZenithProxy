package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryPacket;
import com.zenith.event.proxy.ServerConnectionRemovedEvent;
import com.zenith.feature.forwarding.handlers.inbound.ForwardingHandshakeHandler;
import com.zenith.feature.forwarding.handlers.inbound.ForwardingLoginQueryResponseHandler;
import com.zenith.feature.forwarding.handlers.outbound.ForwardingAddPlayerHandler;
import com.zenith.feature.forwarding.handlers.outbound.ForwardingGameProfileHandler;
import com.zenith.feature.forwarding.handlers.outbound.ForwardingPlayerInfoRemoveHandler;
import com.zenith.feature.forwarding.handlers.outbound.ForwardingPlayerInfoUpdateHandler;
import com.zenith.module.Module;
import com.zenith.network.registry.PacketHandlerCodec;
import com.zenith.network.registry.PacketHandlerStateCodec;
import com.zenith.network.registry.ZenithHandlerCodec;
import com.zenith.network.server.ServerConnection;
import net.kyori.adventure.key.Key;

import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.zenith.Shared.*;

public class ProxyForwarding extends Module {
    public static final byte VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION = (byte) 1;
    public static final Key VELOCITY_PLAYER_INFO_CHANNEL = Key.key("velocity", "player_info");
    public static final int VELOCITY_QUERY_ID = 1;
    private PacketHandlerCodec codec;

    private final Map<ServerConnection, ForwardedInfo> pendingForwardedInfo = new ConcurrentHashMap<>();

    public ProxyForwarding() {
        initializeHandlers();
    }

    private void initializeHandlers() {
        codec = PacketHandlerCodec.builder()
            .setId("proxy-forwarding")
            .setPriority(500)
            .setLogger(MODULE_LOG)
            .state(ProtocolState.HANDSHAKE, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ClientIntentionPacket.class, new ForwardingHandshakeHandler())
                .build())
            .state(ProtocolState.LOGIN, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerInbound(ServerboundCustomQueryPacket.class, new ForwardingLoginQueryResponseHandler())
                .registerOutbound(ClientboundGameProfilePacket.class, new ForwardingGameProfileHandler())
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.<ServerConnection>builder()
                .registerOutbound(ClientboundPlayerInfoUpdatePacket.class, new ForwardingPlayerInfoUpdateHandler())
                .registerOutbound(ClientboundPlayerInfoRemovePacket.class, new ForwardingPlayerInfoRemoveHandler())
                .registerOutbound(ClientboundAddPlayerPacket.class, new ForwardingAddPlayerHandler())
                .build())
            .build();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this,
                            EventConsumer.of(ServerConnectionRemovedEvent.class, this::onServerConnectionRemoved)
        );
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.server.extra.proxyForwarding.enabled;
    }

    @Override
    public void onEnable() {
        ZenithHandlerCodec.SERVER_REGISTRY.register(codec);
    }

    @Override
    public void onDisable() {
        ZenithHandlerCodec.SERVER_REGISTRY.unregister(codec);
    }

    public ForwardedInfo popForwardedInfo(final ServerConnection session) {
        return this.pendingForwardedInfo.remove(session);
    }

    public void setForwardedInfo(final ServerConnection session, final ForwardedInfo info) {
        this.pendingForwardedInfo.put(session, info);
    }

    private void onServerConnectionRemoved(final ServerConnectionRemovedEvent event) {
        this.pendingForwardedInfo.remove(event.serverConnection());
    }

    public static UUID getFakeUuid(UUID original) {
        return UUID.nameUUIDFromBytes(("SpoofedId:" + original).getBytes());
    }

    public record ForwardedInfo(GameProfile profile, SocketAddress remoteAddress) {
    }
}
