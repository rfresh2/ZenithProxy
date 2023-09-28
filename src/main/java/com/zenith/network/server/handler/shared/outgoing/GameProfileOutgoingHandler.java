package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.NonWhitelistedPlayerConnectedEvent;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Wait;
import lombok.NonNull;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class GameProfileOutgoingHandler implements OutgoingHandler<ClientboundGameProfilePacket, ServerConnection> {
    @Override
    public ClientboundGameProfilePacket apply(@NonNull ClientboundGameProfilePacket packet, @NonNull ServerConnection session) {
        try {
            final GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            if (isNull(clientGameProfile)) {
                session.disconnect("Failed to Login");
                return null;
            }
            if (CONFIG.server.extra.whitelist.enable && !WHITELIST_MANAGER.isProfileWhitelisted(clientGameProfile)) {
                if (CONFIG.server.spectator.allowSpectator && WHITELIST_MANAGER.isProfileSpectatorWhitelisted(clientGameProfile)) {
                    session.setOnlySpectator(true);
                } else {
                    session.setWhitelistChecked(false);
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
                        Proxy.getInstance().connect();
                    } else {
                        session.disconnect("Not connected to server!");
                    }
                }
            }

            if (!Wait.waitUntilCondition(() -> CACHE.getProfileCache().getProfile() != null, 3)) {
                session.disconnect("Timed out waiting for the proxy to login");
                return null;
            }
            SERVER_LOG.debug("User UUID: {}\nBot UUID: {}", packet.getProfile().getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
            session.getProfileCache().setProfile(packet.getProfile());
            if (!session.isOnlySpectator() && Proxy.getInstance().getCurrentPlayer().compareAndSet(null, session)) {
                session.setSpectator(false);
                return new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile());
            } else {
                SERVER_LOG.info("Logging in {} [{}] as spectator", packet.getProfile().getName(), packet.getProfile().getId().toString());
                session.setSpectator(true);
                final GameProfile spectatorFakeProfile = new GameProfile(CONFIG.server.spectator.spectatorUUID,
                                                                         packet.getProfile().getName());
                session.getSpectatorFakeProfileCache().setProfile(spectatorFakeProfile);
                return new ClientboundGameProfilePacket(spectatorFakeProfile);
            }
        } catch (final Throwable e) {
            session.disconnect("Login Failed", e);
            return null;
        }
    }
}
