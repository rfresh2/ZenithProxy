package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;

public class SHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerConnection> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerConnection session) {
        session.setUsername(packet.getUsername());
        if (session.getFlag(MinecraftConstants.VERIFY_USERS_KEY, true)) {
            session.send(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge()));
        } else {
            SCHEDULED_EXECUTOR_SERVICE.execute(new UserAuthTask(session, null));
        }
        return null;
    }
}
