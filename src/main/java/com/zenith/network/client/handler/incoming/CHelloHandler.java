package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

import static com.zenith.Shared.CLIENT_LOG;

public class CHelloHandler implements PacketHandler<ClientboundHelloPacket, ClientSession> {
    @Override
    public ClientboundHelloPacket apply(final ClientboundHelloPacket packet, final ClientSession session) {
        GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

        if (profile == null || accessToken == null) {
            session.disconnect("No Profile or Access Token provided.");
            return null;
        }
        SecretKey key;
        try {
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(128);
            key = gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            session.disconnect("Failed to generate shared key.");
            CLIENT_LOG.error("Failed to generate shared key.", e);
            return null;
        }

        SessionService sessionService = session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
        String serverId = sessionService.getServerId(packet.getServerId(), packet.getPublicKey(), key);
        try {
            sessionService.joinServer(profile, accessToken, serverId);
        } catch (ServiceUnavailableException e) {
            session.disconnect("Login failed: Authentication service unavailable.", e);
            return null;
        } catch (InvalidCredentialsException e) {
            session.disconnect("Login failed: Invalid login session.", e);
            return null;
        } catch (RequestException e) {
            session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
            return null;
        }

        session.send(new ServerboundKeyPacket(packet.getPublicKey(), key, packet.getChallenge()));
        session.enableEncryption(key);
        return null;
    }
}
