package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;

public class PostOutgoingClientIntentionHandler implements PostOutgoingPacketHandler<ClientIntentionPacket, ClientSession> {
    @Override
    public void accept(final ClientIntentionPacket packet, final ClientSession session) {
        session.getPacketProtocol().setState(ProtocolState.LOGIN);
        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        session.send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
    }
}
