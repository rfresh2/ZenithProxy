package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;

public class PostOutgoingClientIntentionHandler implements PostOutgoingPacketHandler<ClientIntentionPacket, ClientSession> {
    @Override
    public void accept(final ClientIntentionPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.LOGIN);
        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        session.send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
    }
}
