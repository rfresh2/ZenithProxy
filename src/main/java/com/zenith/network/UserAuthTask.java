package com.zenith.network;

import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;

import javax.crypto.SecretKey;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.CONFIG;

public class UserAuthTask implements Runnable {
    private final ServerSession session;
    private final SecretKey key;

    public UserAuthTask(ServerSession session, SecretKey key) {
        this.key = key;
        this.session = session;
    }

    @Override
    public void run() {
        GameProfile profile;
        if (this.key != null) {
            final Optional<GameProfile> response = SessionServerApi.INSTANCE.hasJoined(
                session.getUsername(),
                SessionServerApi.INSTANCE.getSharedSecret(session.getServerId(),
                                               session.getKeyPair().getPublic(),
                                               this.key));
            if (response.isEmpty()) {
                this.session.disconnect("Failed to verify username.");
                return;
            }
            profile = response.get();
        } else {
            if (CONFIG.server.verifyUsers) {
                this.session.disconnect("No encryption key!");
                return;
            }
            // blindly trusting the player's requested UUID if present
            final var uuid = session.getLoginProfileUUID() == null
                ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getUsername()).getBytes())
                : session.getLoginProfileUUID();
            profile = new GameProfile(uuid, session.getUsername());
        }

        this.session.setFlag(MinecraftConstants.PROFILE_KEY, profile);

        final var threshold = CONFIG.server.compressionThreshold;
        if (threshold >= 0) {
            this.session.send(new ClientboundLoginCompressionPacket(threshold));
        } else {
            session.setCompressionThreshold(threshold, CONFIG.server.compressionLevel, true);
            session.send(new ClientboundGameProfilePacket(profile));
        }
    }
}
