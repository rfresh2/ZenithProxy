package com.zenith.feature.extrachat;

import com.zenith.Proxy;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;

import static com.zenith.Globals.CONFIG;

public class ECSetActionBarTextHandler implements PacketHandler<ClientboundSetActionBarTextPacket, ServerSession> {
    @Override
    public ClientboundSetActionBarTextPacket apply(final ClientboundSetActionBarTextPacket packet, final ServerSession session) {
        if (CONFIG.client.extra.chat.hide2b2tActionBarText
            && Proxy.getInstance().isOn2b2t()
            && !Proxy.getInstance().isInQueue()
            && ComponentSerializer.serializePlain(packet.getText()).toLowerCase().contains("2b2t.org")
        ) {
            return null;
        }
        return packet;
    }
}
