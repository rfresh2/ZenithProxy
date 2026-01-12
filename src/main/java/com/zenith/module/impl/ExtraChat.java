package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.event.queue.QueuePositionUpdateEvent;
import com.zenith.event.server.ServerPlayerConnectedEvent;
import com.zenith.event.server.ServerPlayerDisconnectedEvent;
import com.zenith.feature.extrachat.*;
import com.zenith.module.api.Module;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.List;
import java.util.regex.Pattern;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CHAT_LOG;
import static com.zenith.Globals.CONFIG;
import static com.zenith.util.ComponentSerializer.minimessage;
import static java.util.Objects.nonNull;

public class ExtraChat extends Module {

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.chat.enabled;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ServerPlayerConnectedEvent.class, this::handleServerPlayerConnected),
            of(ServerPlayerDisconnectedEvent.class, this::handleServerPlayerDisconnected),
            of(QueuePositionUpdateEvent.class, this::handleQueuePositionUpdate)
        );
    }

    @Override
    public PacketHandlerCodec registerServerPacketHandlerCodec() {
        return PacketHandlerCodec.serverBuilder()
            .setId("extra-chat")
            .setPriority(-10)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.serverBuilder()
                .outbound(ClientboundSystemChatPacket.class, new ECSystemChatOutgoingHandler())
                .outbound(ClientboundPlayerChatPacket.class, new ECPlayerChatOutgoingHandler())
                .outbound(ClientboundSetActionBarTextPacket.class, new ECSetActionBarTextHandler())
                .inbound(ServerboundChatCommandPacket.class, new ECChatCommandIncomingHandler())
                .inbound(ServerboundChatCommandSignedPacket.class, new ECSignedChatCommandIncomingHandler())
                .build())
            .build();
    }


    @Override
    public PacketHandlerCodec registerClientPacketHandlerCodec() {
        return PacketHandlerCodec.clientBuilder()
            .setId("extra-chat")
            .setPriority(-10)
            .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                .outbound(ServerboundChatPacket.class, new ECClientOutgoingChatHandler())
                .build())
            .build();
    }

    private void handleServerPlayerDisconnected(ServerPlayerDisconnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.sendAsyncMessage(minimessage("<aqua>" + event.playerEntry().getName() + "<yellow> disconnected"));
    }

    private void handleServerPlayerConnected(ServerPlayerConnectedEvent event) {
        if (!CONFIG.client.extra.chat.showConnectionMessages) return;
        var serverConnection = Proxy.getInstance().getCurrentPlayer().get();
        if (nonNull(serverConnection) && serverConnection.isLoggedIn())
            serverConnection.sendAsyncMessage(minimessage("<aqua>" + event.playerEntry().getName() + "<yellow> connected"));
    }

    private void handleQueuePositionUpdate(QueuePositionUpdateEvent event) {
        if (!CONFIG.client.extra.logChatMessages || !CONFIG.client.extra.logOnlyQueuePositionUpdates) return;
        CHAT_LOG.info(Component.text("Position in queue: " + event.position()).color(NamedTextColor.GOLD));
    }

    private static final Pattern urlPattern = Pattern.compile("(?i)(?<link>[a-z0-9:/]+(www\\.)?[-a-z0-9@:%._+~#=]+\\.[a-z0-9()]{1,6}\\b([-a-z0-9()@:%_+.~#?&/=]*))");

    public Component insertClickableLinks(Component component) {
        var replacementConfig = TextReplacementConfig.builder()
            .match(urlPattern)
            .replacement((matchResult, builder) -> {
                var link = matchResult.group();
                var lowerCase = link.toLowerCase();
                var httpLink = lowerCase.startsWith("http://") || link.startsWith("https://")
                    ? lowerCase
                    : "https://" + lowerCase;
                return Component.text(link)
                    .clickEvent(ClickEvent.openUrl(httpLink))
                    .hoverEvent(HoverEvent.showText(Component.text(httpLink).color(NamedTextColor.GRAY)));
            })
            .build();
        return component.replaceText(replacementConfig);
    }
}
