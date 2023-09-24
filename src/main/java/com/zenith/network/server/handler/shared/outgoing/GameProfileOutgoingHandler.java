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
            if (!Proxy.getInstance().isConnected()) {
                if (CONFIG.client.extra.autoConnectOnLogin && !session.isOnlySpectator()) {
                    Proxy.getInstance().connect();
                } else {
                    session.disconnect("Not connected to server!");
                }
            }

            // profile could be null at this point?
            int tryCount = 0;
            while (tryCount < 3 && CACHE.getProfileCache().getProfile() == null) {
                Wait.waitALittleMs(500);
                tryCount++;
            }
            if (CACHE.getProfileCache().getProfile() == null) {
                session.disconnect(MANUAL_DISCONNECT);
                return null;
            } else {
                SERVER_LOG.debug("User UUID: {}\nBot UUID: {}", packet.getProfile().getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
                session.getProfileCache().setProfile(packet.getProfile());
                if (isNull(session.getProxy().getCurrentPlayer().get())) {
                    return new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile());
                } else {
                    return new ClientboundGameProfilePacket(session.getProfileCache().getProfile());
                }
            }
        } catch (final Throwable e) {
            session.disconnect("Login Failed", e);
            return null;
        }
    }
}
