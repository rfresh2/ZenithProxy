package com.zenith.network.server.handler.shared.incoming;

import com.zenith.feature.ratelimiter.RateLimiter;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class IntentionHandler implements PacketHandler<ClientIntentionPacket, ServerSession> {
    private final RateLimiter rateLimiter = new RateLimiter(CONFIG.server.rateLimiter.rateLimitSeconds);

    @Override
    public ClientIntentionPacket apply(final ClientIntentionPacket packet, final ServerSession session) {
        MinecraftProtocol protocol = session.getPacketProtocol();
        switch (packet.getIntent()) {
            case STATUS -> {
                protocol.setOutboundState(ProtocolState.STATUS);
                session.switchInboundState(ProtocolState.STATUS);
            }
            case LOGIN -> {
                if (handleLogin(packet, session, protocol)) return null;
            }
            case TRANSFER -> {
                SERVER_LOG.info("Transfer request from {}", session.getRemoteAddress());
                session.setTransferring(true);
                if (!CONFIG.server.acceptTransfers) {
                    session.disconnect("Transfers are disabled.");
                    return null;
                }
                if (handleLogin(packet, session, protocol)) return null;
            }
            default -> session.disconnect("Invalid client intention: " + packet.getIntent());
        }
        session.setProtocolVersion(packet.getProtocolVersion());
        return null;
    }

    private boolean handleLogin(final ClientIntentionPacket packet, final ServerSession session, final MinecraftProtocol protocol) {
        protocol.setOutboundState(ProtocolState.LOGIN);
        if (CONFIG.server.rateLimiter.enabled && rateLimiter.isRateLimited(session)) {
            SERVER_LOG.info("Disconnecting {} due to rate limiting.", session.getRemoteAddress());
            session.disconnect("Login Rate Limited.");
            return true;
        }
        if (packet.getProtocolVersion() > protocol.getCodec().getProtocolVersion()) {
            SERVER_LOG.info("Disconnecting {} due to outdated server version.", session.getRemoteAddress());
            session.disconnect("Outdated server! I'm still on " + protocol.getCodec()
                .getMinecraftVersion() + ".");
            return true;
        } else if (packet.getProtocolVersion() < protocol.getCodec().getProtocolVersion()) {
            SERVER_LOG.info("Disconnecting {} due to outdated client version.", session.getRemoteAddress());
            session.disconnect("Outdated client! Please use " + protocol.getCodec()
                .getMinecraftVersion() + ".");
            return true;
        }
        session.switchInboundState(ProtocolState.LOGIN);
        return false;
    }
}
