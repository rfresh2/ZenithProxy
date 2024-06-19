package com.zenith.network.client.handler.incoming;

import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;

import javax.crypto.SecretKey;

public class CHelloHandler implements PacketHandler<ClientboundHelloPacket, ClientSession> {
    @Override
    public ClientboundHelloPacket apply(final ClientboundHelloPacket packet, final ClientSession session) {
        final GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        final String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

        if (profile == null || accessToken == null) {
            session.disconnect("No Profile or Access Token provided.");
            return null;
        }
        final SecretKey key = SessionServerApi.INSTANCE.generateClientKey();
        if (key == null) {
            session.disconnect("Failed to generate secret key.");
            return null;
        }
        final String sharedSecret = SessionServerApi.INSTANCE.getSharedSecret(packet.getServerId(), packet.getPublicKey(), key);
        try {
            SessionServerApi.INSTANCE.joinServer(profile.getId(), accessToken, sharedSecret);
        } catch (Exception e) {
            session.disconnect("Login failed: Authentication service unavailable.", e);
            return null;
        }
        session.send(new ServerboundKeyPacket(packet.getPublicKey(), key, packet.getChallenge()));
        session.enableEncryption(key);
        return null;
    }
}
