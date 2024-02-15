package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import javax.crypto.SecretKey;

import static com.zenith.Shared.SESSION_SERVER_API;

public class CHelloHandler implements PacketHandler<ClientboundHelloPacket, ClientSession> {
    @Override
    public ClientboundHelloPacket apply(final ClientboundHelloPacket packet, final ClientSession session) {
        final GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        final String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

        if (profile == null || accessToken == null) {
            session.disconnect("No Profile or Access Token provided.");
            return null;
        }
        final SecretKey key = SESSION_SERVER_API.generateClientKey();
        if (key == null) {
            session.disconnect("Failed to generate secret key.");
            return null;
        }
        final String sharedSecret = SESSION_SERVER_API.getSharedSecret(packet.getServerId(), packet.getPublicKey(), key);
        try {
            SESSION_SERVER_API.joinServer(profile.getId(), accessToken, sharedSecret);
        } catch (Exception e) {
            session.disconnect("Login failed: Authentication service unavailable.", e);
            return null;
        }
        session.send(new ServerboundKeyPacket(packet.getPublicKey(), key, packet.getChallenge()));
        session.enableEncryption(key);
        return null;
    }
}
