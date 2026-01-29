package com.zenith.feature.extrachat;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import com.zenith.util.ChatUtil;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.UUID;

import static com.zenith.Globals.CONFIG;

public class ECClientOutgoingChatHandler implements PacketHandler<ServerboundChatPacket, ClientSession> {
    @Override
    public ServerboundChatPacket apply(final ServerboundChatPacket packet, final ClientSession session) {
        String message = packet.getMessage();
        if (message.startsWith("/")) return packet;
        if (CONFIG.client.extra.chat.prefixChats && !CONFIG.client.extra.chat.prefix.isEmpty()) {
            message = ChatUtil.constrainChatMessageSize(CONFIG.client.extra.chat.prefix + " " + message, false);
        }
        if (CONFIG.client.extra.chat.suffixChats) {
            String suffix = "";
            if (CONFIG.client.extra.chat.randomSuffix) {
                suffix = UUID.randomUUID().toString().substring(0, 6);
            } else if (!CONFIG.client.extra.chat.suffix.isEmpty()) {
                suffix = CONFIG.client.extra.chat.suffix;
            }
            if (!suffix.isEmpty()) {
                message = ChatUtil.constrainChatMessageSize(message + " " + suffix, true);
            }
        }
        if (message == packet.getMessage()) return packet;
        return new ServerboundChatPacket(message);
    }
}
