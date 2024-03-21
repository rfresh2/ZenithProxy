package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.zenith.network.UserAuthTask;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.Arrays;

import static com.zenith.Shared.EXECUTOR;

public class KeyHandler implements PacketHandler<ServerboundKeyPacket, ServerConnection> {

    @Override
    public ServerboundKeyPacket apply(final ServerboundKeyPacket packet, final ServerConnection session) {
        PrivateKey privateKey = session.getKeyPair().getPrivate();
        if (!Arrays.equals(session.getChallenge(), packet.getEncryptedChallenge(privateKey))) {
            session.disconnect("Invalid challenge!");
            return null;
        }
        SecretKey key = packet.getSecretKey(privateKey);
        session.enableEncryption(key);
        EXECUTOR.execute(new UserAuthTask(session, key));
        return null;
    }
}
