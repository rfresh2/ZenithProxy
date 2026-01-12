package com.zenith.feature.extrachat;

import com.zenith.Proxy;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import static com.zenith.Globals.CONFIG;

public class ECSignedChatCommandIncomingHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ServerSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ServerSession session) {
        if (!Proxy.getInstance().isOn2b2t()) return packet;
        if (session.isSpectator()) return packet;
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (!CONFIG.client.extra.chat.replace2b2tChatCommands) {
            if (!CONFIG.client.extra.chat.ignoreReplace2b2tChatCommandWhileDatabaseOn || !CONFIG.database.enabled) {
                return packet;
            }
        }
        return ECChatCommandIncomingHandler.handleExtraChatCommand(command, session) ? packet : null;
    }
}
