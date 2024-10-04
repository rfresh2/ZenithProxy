package com.zenith.network.client.handler.incoming;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;

import static com.zenith.Shared.*;

public class PlayerChatHandler implements PacketHandler<ClientboundPlayerChatPacket, ClientSession> {

    @Override
    public ClientboundPlayerChatPacket apply(ClientboundPlayerChatPacket packet, ClientSession session) {
        // we shouldn't receive any of these packets on 2b or any anarchy server due to no chat reports plugins
        // and this does not pass these packets through to our normal chat handlers
        // so no chat relay, chat events, and other stuff
        var sender = CACHE.getTabListCache().get(packet.getSender()).map(PlayerListEntry::getName).orElse("?");
        var component = Component.text("<" + sender + "> " + packet.getContent()).color(NamedTextColor.WHITE);
        CHAT_LOG.info(ComponentSerializer.serializeJson(component));
        if (packet.getUnsignedContent() != null) {
            CLIENT_LOG.info("Chat with unsigned content: {}", ComponentSerializer.serializePlain(packet.getUnsignedContent()));
        }
        return packet;
    }
}
