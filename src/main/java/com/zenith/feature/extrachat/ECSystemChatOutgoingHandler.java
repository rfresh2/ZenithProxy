package com.zenith.feature.extrachat;

import com.zenith.Proxy;
import com.zenith.feature.chatschema.ChatSchemaParser;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.module.impl.ExtraChat;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Objects;
import java.util.Optional;

import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class ECSystemChatOutgoingHandler implements PacketHandler<ClientboundSystemChatPacket, ServerSession> {

    @Override
    public ClientboundSystemChatPacket apply(ClientboundSystemChatPacket packet, ServerSession session) {
        if (packet.isOverlay()) return packet;
        try {
            final Component component = packet.getContent();
            final String message = ComponentSerializer.serializePlain(component);

            if (CONFIG.client.extra.chat.hideDeathMessages && Proxy.getInstance().isOn2b2t() && isDeathMessage(component, message)) {
                return null;
            }

            var chatParseResult = ChatSchemaParser.parse(message);
            if (chatParseResult != null) {
                switch (chatParseResult.type()) {
                    case PUBLIC_CHAT -> {
                        if (CONFIG.client.extra.chat.hideChat) {
                            return null;
                        }
                        if (PLAYER_LISTS.getIgnoreList().contains(chatParseResult.sender().getProfileId())) {
                            return null;
                        }
                    }
                    case WHISPER_INBOUND -> {
                        if (CONFIG.client.extra.chat.hideWhispers) {
                            return null;
                        }
                        if (PLAYER_LISTS.getIgnoreList().contains(chatParseResult.sender().getProfileId())) {
                            return null;
                        }
                    }
                    // less confusing for users if we show outbound whispers imo
//                    case WHISPER_OUTBOUND -> {
//                        if (CONFIG.client.extra.chat.hideWhispers) {
//                            return null;
//                        }
//                    }
                }
            }
            if (CONFIG.client.extra.chat.insertClickableLinks && !packet.isOverlay()) {
                return new ClientboundSystemChatPacket(MODULE.get(ExtraChat.class).insertClickableLinks(packet.getContent()), packet.isOverlay());
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed to parse chat message in ExtraChatSystemChatOutgoingHandler: {}",
                             ComponentSerializer.serializePlain(packet.getContent()),
                             e);
        }
        return packet;
    }

    private boolean isDeathMessage(final Component component, final String messageRaw) {
        return parseDeathMessage2b2t(component, messageRaw).isPresent();
    }

    private Optional<DeathMessageParseResult> parseDeathMessage2b2t(final Component component, final String messageString) {
        if (component.children().stream().anyMatch(child -> nonNull(child.color())
            && Objects.equals(child.color(), NamedTextColor.DARK_AQUA))) { // death message color on 2b
            return DeathMessagesParser.INSTANCE.parse(component, messageString);
        }
        return Optional.empty();
    }
}
