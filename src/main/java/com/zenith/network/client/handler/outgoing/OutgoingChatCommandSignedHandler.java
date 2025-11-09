package com.zenith.network.client.handler.outgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;

import java.util.BitSet;

import static com.zenith.Globals.CACHE;

public class OutgoingChatCommandSignedHandler implements PacketHandler<ServerboundChatCommandSignedPacket, ClientSession> {
    @Override
    public ServerboundChatCommandSignedPacket apply(final ServerboundChatCommandSignedPacket packet, final ClientSession session) {
        var lastChatTimestamp = CACHE.getChatCache().getLastChatTimestamp();
        var currentTime = System.currentTimeMillis();
        var packetTime = Math.max(lastChatTimestamp+1, currentTime);
        packet.setTimeStamp(packetTime);
        CACHE.getChatCache().setLastChatTimestamp(packetTime);
        if (CACHE.getChatCache().canUseChatSigning()) {
            var signedCommand = new ServerboundChatCommandSignedPacket(packet.getCommand(), packet.getTimeStamp(), 0, packet.getSignatures(), 0, BitSet.valueOf(new byte[20]));
            CACHE.getChatCache().getChatSession().sign(signedCommand);
            return signedCommand;
        }
        return packet;
    }
}
