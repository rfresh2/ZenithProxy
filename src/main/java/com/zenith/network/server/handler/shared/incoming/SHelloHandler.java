package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.UserAuthTask;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ChatUtil;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EXECUTOR;

public class SHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerSession> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerSession session) {
        if (CONFIG.server.strictLoginPacketSequence && session.getLoginState() != ServerSession.LoginState.HELLO) {
            session.disconnect("Unexpected hello packet");
            return null;
        }
        if (!ChatUtil.isValidPlayerName(packet.getUsername())) {
            session.disconnect("Invalid username.");
            return null;
        }
        session.setUsername(packet.getUsername());
        session.setLoginProfileUUID(packet.getProfileId());
        if (session.isTransferring()) {
            session.setLoginState(ServerSession.LoginState.WAITING_FOR_COOKIES);
            session.getCookieCache().getPackets(session::sendAsync, session);
        } else {
            if (CONFIG.server.verifyUsers) {
                session.setLoginState(ServerSession.LoginState.KEY);
                session.sendAsync(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge(), true));
            } else {
                EXECUTOR.execute(new UserAuthTask(session, null));
            }
        }
        return null;
    }
}
