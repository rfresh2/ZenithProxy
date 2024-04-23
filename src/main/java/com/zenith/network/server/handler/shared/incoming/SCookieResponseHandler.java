package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ServerboundCookieResponsePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.EXECUTOR;
import static com.zenith.Shared.SERVER_LOG;

public class SCookieResponseHandler implements PacketHandler<ServerboundCookieResponsePacket, ServerConnection> {
    @Override
    public ServerboundCookieResponsePacket apply(final ServerboundCookieResponsePacket packet, final ServerConnection session) {
        if (session.isTransferring() && !session.isConfigured()) {
            if (!packet.getKey().startsWith("minecraft:")) {
                SERVER_LOG.debug("Received unexpected cookie response: {}", packet.getKey());
                return null;
            }
            var cookieKey = packet.getKey().split("minecraft:")[1];
            // todo: refactor this logic so we can cleanly add or remove cookies as needed
            if (cookieKey.equals(ServerConnection.COOKIE_ZENITH_TRANSFER_SRC)) {
                session.setReceivedTransferSrcCookie(true);
            } else if (cookieKey.equals(ServerConnection.COOKIE_ZENITH_SPECTATOR)) {
                session.setReceivedSpectatorCookie(true);
            } else {
                SERVER_LOG.debug("Received unexpected cookie: {}", cookieKey);
                return null;
            }
            var payload = packet.getPayload();
            if (payload != null) {
                try {
                    var value = new String(payload);
                    session.getCookies().put(cookieKey, value);
                } catch (final Throwable e) {
                    SERVER_LOG.debug("Unable to parse cookie response to string for key: {}", cookieKey, e);
                    return null;
                }
            }
            if (session.isReceivedTransferSrcCookie() && session.isReceivedSpectatorCookie()) {
                if (session.getFlag(MinecraftConstants.VERIFY_USERS_KEY, true)) {
                    session.send(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge(), true));
                } else {
                    EXECUTOR.execute(new UserAuthTask(session, null));
                }
            }
        }
        SERVER_LOG.debug("Received unrequested cookie response: {}", packet.getKey());
        // shouldn't ever get here
        // should these packets ever be passed through?
        return null;
    }
}
