package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ServerboundCookieResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;

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
            session.getCookieCache().handleCookieResponse(cookieKey, packet.getPayload());

            if (session.getCookieCache().receivedAllCookieResponses()) {
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