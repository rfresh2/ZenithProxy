package com.zenith.feature.forwarding.handlers.inbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryPacket;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.zenith.Shared.CONFIG;

public class ForwardingLoginQueryResponseHandler implements PacketHandler<ServerboundCustomQueryPacket, ServerConnection> {
    @Override
    public ServerboundCustomQueryPacket apply(final ServerboundCustomQueryPacket packet, final ServerConnection session) {
        if (packet.getMessageId() == ProxyForwarding.VELOCITY_QUERY_ID) {
            final byte[] data = packet.getData();
            if (data == null) {
                session.disconnect("This server requires you to connect with Velocity.");
                return packet;
            }

            final ByteBuf buf = Unpooled.wrappedBuffer(data);

            if (!checkIntegrity(buf)) {
                session.disconnect("Unable to verify player details");
                return packet;
            }

            final int version = session.getCodecHelper().readVarInt(buf);
            if (version > ProxyForwarding.VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION) {
                session.disconnect("Unsupported forwarding version " + version + ", wanted up to " + ProxyForwarding.VELOCITY_MAX_SUPPORTED_FORWARDING_VERSION);
                return packet;
            }

            session.getCodecHelper().readString(buf); // spoofed address, TODO use?

            final GameProfile profile = createProfile(buf, (MinecraftCodecHelper) session.getCodecHelper());
            session.setSpoofedUuid(profile.getId());
            session.setSpoofedProperties(profile.getProperties());
        }
        return packet;
    }

    private static GameProfile createProfile(final ByteBuf buf, final MinecraftCodecHelper helper) {
        final GameProfile profile = new GameProfile(helper.readUUID(buf), helper.readString(buf, 16));

        final int properties = helper.readVarInt(buf);
        for (int i = 0; i < properties; i++) {
            final String name = helper.readString(buf);
            final String value = helper.readString(buf);
            final String signature = helper.readNullable(buf, helper::readString);

            profile.getProperties().add(new GameProfile.Property(name, value, signature));
        }

        return profile;
    }

    private static boolean checkIntegrity(final ByteBuf buf) {
        final byte[] signature = new byte[32];
        buf.readBytes(signature);

        final byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);

        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CONFIG.client.extra.proxyForwarding.secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            final byte[] mySignature = mac.doFinal(data);
            if (!MessageDigest.isEqual(signature, mySignature)) {
                return false;
            }
        } catch (final InvalidKeyException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        return true;
    }
}
