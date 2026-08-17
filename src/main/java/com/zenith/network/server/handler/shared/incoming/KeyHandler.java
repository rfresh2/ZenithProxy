package com.zenith.network.server.handler.shared.incoming;

import com.zenith.network.UserAuthTask;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.Arrays;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EXECUTOR;

public class KeyHandler implements PacketHandler<ServerboundKeyPacket, ServerSession> {

    @Override
    public ServerboundKeyPacket apply(final ServerboundKeyPacket packet, final ServerSession session) {
        if (CONFIG.server.strictLoginPacketSequence && session.getLoginState() != ServerSession.LoginState.KEY) {
            session.disconnect("Unexpected key packet");
            return null;
        }
        PrivateKey privateKey = session.getKeyPair().getPrivate();
        if (!Arrays.equals(session.getChallenge(), packet.getEncryptedChallenge(privateKey))) {
            session.disconnect("Invalid challenge!");
            return null;
        }
        SecretKey key = packet.getSecretKey(privateKey);
        session.enableEncryption(key);
        session.setLoginState(ServerSession.LoginState.AUTHENTICATING);
        EXECUTOR.execute(new UserAuthTask(session, key));
        return null;
    }
}
