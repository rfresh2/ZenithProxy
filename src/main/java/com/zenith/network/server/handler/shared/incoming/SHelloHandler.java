package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCookieRequestPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

import static com.zenith.Shared.EXECUTOR;

public class SHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerConnection session) {
        session.setUsername(packet.getUsername());
        session.setLoginProfileUUID(packet.getProfileId());
        if (session.isTransferring()) {
            // TODO: see how viaversion interacts with this sequence
            //  it seems to be legal for clients to not send a response to the cookie request, at which point we stall
            //  in this login sequence forever
            session.sendAsync(new ClientboundCookieRequestPacket(ServerConnection.COOKIE_ZENITH_TRANSFER_SRC));
            session.sendAsync(new ClientboundCookieRequestPacket(ServerConnection.COOKIE_ZENITH_SPECTATOR));
        } else {
            if (session.getFlag(MinecraftConstants.VERIFY_USERS_KEY, true)) {
                session.send(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge(), true));
            } else {
                EXECUTOR.execute(new UserAuthTask(session, null));
            }
        }
        return null;
    }
}
