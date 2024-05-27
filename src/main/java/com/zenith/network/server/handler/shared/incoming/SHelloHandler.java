package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EXECUTOR;

public class SHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerConnection session) {
        session.setUsername(packet.getUsername());
        session.setLoginProfileUUID(packet.getProfileId());
        if (session.isTransferring())
            // TODO: see how viaversion interacts with this sequence
            //  it seems to be legal for clients to not send a response to the cookie request, at which point we stall
            //  in this login sequence forever
            session.getCookieCache().getPackets(session::sendAsync);
        else {
            if (CONFIG.server.verifyUsers)
                session.sendAsync(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge(), true));
            else
                EXECUTOR.execute(new UserAuthTask(session, null));
        }
        return null;
    }
}
