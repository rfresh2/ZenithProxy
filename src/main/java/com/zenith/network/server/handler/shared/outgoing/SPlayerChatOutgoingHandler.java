package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.mc.chat_type.ChatTypeRegistry;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Objects;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.SERVER_LOG;

public class SPlayerChatOutgoingHandler implements PacketHandler<ClientboundPlayerChatPacket, ServerSession> {
    @Override
    public ClientboundPlayerChatPacket apply(final ClientboundPlayerChatPacket packet, final ServerSession session) {
        return switch (CONFIG.server.chatSigning.mode) {
            case PASSTHROUGH -> packet;
            case DISGUISED -> {
                var disguised = new ClientboundDisguisedChatPacket(
                    Objects.requireNonNullElseGet(packet.getUnsignedContent(), () -> Component.text(packet.getContent())),
                    packet.getChatType(),
                    packet.getName(),
                    packet.getTargetName()
                );
                session.send(disguised);
                yield null;
            }
            case SYSTEM -> {
                var chatType = ChatTypeRegistry.REGISTRY.get(packet.getChatType().id());
                if (chatType != null) {
                    TextComponent packetContentComponent = Component.text(packet.getContent());
                    Component chatComponent = chatType.render(
                        packet.getName(),
                        packetContentComponent,
                        packet.getUnsignedContent(),
                        packet.getTargetName());
                    session.send(new ClientboundSystemChatPacket(chatComponent, false));
                    yield null;
                } else {
                    SERVER_LOG.debug("Failed to find chat type for id: {}", packet.getChatType().id());
                    yield packet;
                }
            }
        };
    }
}
