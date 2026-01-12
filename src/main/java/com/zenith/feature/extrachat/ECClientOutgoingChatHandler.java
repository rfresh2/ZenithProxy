package com.zenith.feature.extrachat;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.UUID;

import static com.zenith.Globals.CONFIG;

public class ECClientOutgoingChatHandler implements PacketHandler<ServerboundChatPacket, ClientSession> {
    @Override
    public ServerboundChatPacket apply(final ServerboundChatPacket packet, final ClientSession session) {
        String message = packet.getMessage();
        if (message.startsWith("/")) return packet;
        if (CONFIG.client.extra.chat.prefixChats && !CONFIG.client.extra.chat.prefix.isEmpty()) {
            String prefix = CONFIG.client.extra.chat.prefix + " ";
            if (prefix.length() + message.length() > 256) {
                prefix = prefix.substring(0, 256 - message.length());
            }
            message = prefix + message;
        }
        if (CONFIG.client.extra.chat.suffixChats) {
            if (CONFIG.client.extra.chat.randomSuffix) {
                String suffix = " " + UUID.randomUUID().toString().substring(0, 6);
                if (suffix.length() + message.length() > 256) {
                    suffix = suffix.substring(0, 256 - message.length());
                }
                message = message + suffix;
            } else if (!CONFIG.client.extra.chat.suffix.isEmpty()) {
                String suffix = " " + CONFIG.client.extra.chat.suffix;
                if (suffix.length() + message.length() > 256) {
                    suffix = suffix.substring(0, 256 - message.length());
                }
                message = message + suffix;
            }
        }
        if (message == packet.getMessage()) return packet;
        return new ServerboundChatPacket(message);
    }
}
