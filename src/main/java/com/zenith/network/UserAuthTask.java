package com.zenith.network;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.zenith.network.server.ServerConnection;

import javax.crypto.SecretKey;
import java.util.UUID;

public class UserAuthTask implements Runnable {
    private ServerConnection session;
    private SecretKey key;

    public UserAuthTask(ServerConnection session, SecretKey key) {
        this.key = key;
        this.session = session;
    }

    @Override
    public void run() {
        GameProfile profile;
        if (this.key != null) {
            SessionService sessionService = this.session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY,
                                                                 new SessionService());
            try {
                profile = sessionService.getProfileByServer(session.getUsername(),
                                                            sessionService.getServerId(session.getServerId(),
                                                                                       session.getKeyPair().getPublic(),
                                                                                       this.key));
            } catch (RequestException e) {
                this.session.disconnect("Failed to make session service request.", e);
                return;
            }

            if (profile == null) {
                this.session.disconnect("Failed to verify username.");
            }
        } else {
            profile = new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getUsername()).getBytes()),
                                      session.getUsername());
        }

        this.session.setFlag(MinecraftConstants.PROFILE_KEY, profile);

        int threshold = session.getFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD,
                                        ServerConnection.DEFAULT_COMPRESSION_THRESHOLD);
        this.session.send(new ClientboundLoginCompressionPacket(threshold));
    }
}
