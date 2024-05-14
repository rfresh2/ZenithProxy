package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.Proxy;
import com.zenith.event.proxy.NonWhitelistedPlayerConnectedEvent;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Wait;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class SGameProfileOutgoingHandler implements PacketHandler<ClientboundGameProfilePacket, ServerConnection> {
    // can be anything really, just needs to be unique and not taken by a real player seen in-game
    private static final UUID spectatorFakeUUID = UUID.fromString("c9560dfb-a792-4226-ad06-db1b6dc40b95");

    @Override
    public ClientboundGameProfilePacket apply(@NonNull ClientboundGameProfilePacket packet, @NonNull ServerConnection session) {
        try {
            final GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            if (isNull(clientGameProfile)) {
                session.disconnect("Failed to Login");
                return null;
            }
            if (CONFIG.server.extra.whitelist.enable && !PLAYER_LISTS.getWhitelist().contains(clientGameProfile)) {
                if (CONFIG.server.spectator.allowSpectator && PLAYER_LISTS.getSpectatorWhitelist().contains(clientGameProfile)) {
                    session.setOnlySpectator(true);
                } else {
                    session.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                    SERVER_LOG.warn("Username: {} UUID: {} [{}] tried to connect!", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getRemoteAddress());
                    EVENT_BUS.post(new NonWhitelistedPlayerConnectedEvent(clientGameProfile, session.getRemoteAddress()));
                    return null;
                }
            }
            SERVER_LOG.info("Username: {} UUID: {} [{}] has passed the whitelist check!", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getRemoteAddress());
            session.setWhitelistChecked(true);
            synchronized (this) {
                if (!Proxy.getInstance().isConnected()) {
                    if (CONFIG.client.extra.autoConnectOnLogin && !session.isOnlySpectator()) {
                        try {
                            EXECUTOR.submit(() -> Proxy.getInstance().connect()).get(15, TimeUnit.SECONDS);
                        } catch (final Throwable e) {
                            if (!Proxy.getInstance().isConnected() && !Proxy.getInstance().getLoggingIn().get()) {
                                session.disconnect("Failed to connect to server");
                                return null;
                            }
                            // else, we are either connected or logging in so let's continue and hit the next wait barrier
                            // if we're logging in, most likely the auth failed and is retrying inside a blocking task
                        }
                    } else {
                        session.disconnect("Not connected to server!");
                    }
                }
            }
            if (!Wait.waitUntil(() -> {
                var client = Proxy.getInstance().getClient();
                return client != null
                    && CACHE.getProfileCache().getProfile() != null
                    && (client.isOnline()
                        || (client.isInQueue() && Proxy.getInstance().getQueuePosition() > 1));
            }, 15)) {
                SERVER_LOG.info("Timed out waiting for the proxy to login");
                session.disconnect("Timed out waiting for the proxy to login");
                return null;
            }
            SERVER_LOG.debug("User UUID: {}\nBot UUID: {}", clientGameProfile.getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
            session.getProfileCache().setProfile(clientGameProfile);
            if (!session.isOnlySpectator() && Proxy.getInstance().getCurrentPlayer().compareAndSet(null, session)) {
                return new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile());
            } else {
                if (!CONFIG.server.spectator.allowSpectator) {
                    session.disconnect("Spectator mode is disabled");
                    return null;
                }
                SERVER_LOG.info("Logging in {} [{}] as spectator", clientGameProfile.getName(), clientGameProfile.getId().toString());
                session.setSpectator(true);
                final GameProfile spectatorFakeProfile = new GameProfile(spectatorFakeUUID, clientGameProfile.getName());
                // caching assumes the spectatorUUID is immutable
                if (clientGameProfile.getProperty("textures") == null) {
                    SESSION_SERVER.getProfileAndSkin(clientGameProfile.getId()).ifPresentOrElse(p -> {
                        spectatorFakeProfile.setProperties(p.getProperties());
                    }, () -> {
                        SERVER_LOG.info("Failed getting spectator skin for {} [{}]", clientGameProfile.getName(), clientGameProfile.getId().toString());
                    });
                } else {
                    spectatorFakeProfile.setProperties(clientGameProfile.getProperties());
                }
                session.getSpectatorFakeProfileCache().setProfile(spectatorFakeProfile);
                return new ClientboundGameProfilePacket(spectatorFakeProfile);
            }
        } catch (final Throwable e) {
            session.disconnect("Login Failed", e);
            return null;
        }
    }
}
