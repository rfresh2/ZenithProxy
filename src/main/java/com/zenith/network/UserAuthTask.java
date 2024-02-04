package com.zenith.network;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.zenith.network.server.ServerConnection;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SESSION_SERVER_API;

public class UserAuthTask implements Runnable {
    private final ServerConnection session;
    private final SecretKey key;

    public UserAuthTask(ServerConnection session, SecretKey key) {
        this.key = key;
        this.session = session;
    }

    @Override
    public void run() {
        GameProfile profile;
        if (this.key != null) {
            final Optional<GameProfile> response = SESSION_SERVER_API.hasJoined(
                session.getUsername(),
                SESSION_SERVER_API.getSharedSecret(session.getServerId(),
                                                   session.getKeyPair().getPublic(),
                                                   this.key));
            if (response.isEmpty()) {
                this.session.disconnect("Failed to verify username.");
                return;
            }
            profile = response.get();
        } else {
            profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getUsername()).getBytes()),
                                      session.getUsername());
        }

        this.session.setFlag(MinecraftConstants.PROFILE_KEY, profile);

        int threshold = session.getFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD,
                                        ServerConnection.DEFAULT_COMPRESSION_THRESHOLD);
        if (threshold >= 0) {
            this.session.send(new ClientboundLoginCompressionPacket(threshold));
        } else {
            session.setCompressionThreshold(threshold, CONFIG.server.compressionLevel, true);
            session.send(new ClientboundGameProfilePacket(session.getFlag(MinecraftConstants.PROFILE_KEY)));
        }
    }
}
