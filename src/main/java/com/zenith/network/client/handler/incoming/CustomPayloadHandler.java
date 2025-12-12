package com.zenith.network.client.handler.incoming;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.BrandSerializer;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;
import static com.zenith.Globals.CONFIG;

public class CustomPayloadHandler implements PacketHandler<ClientboundCustomPayloadPacket, ClientSession> {
    public static final CustomPayloadHandler INSTANCE = new CustomPayloadHandler();
    private static byte[] lastPlasmoVoicePacketData;
    private static final List<byte[]> plasmoHandshakePackets = new ArrayList<>();

    @Override
    public ClientboundCustomPayloadPacket apply(ClientboundCustomPayloadPacket packet, ClientSession session) {
        Key channel = packet.getChannel();
        byte[] data = packet.getData();
        if (CONFIG.debug.debugLogs && !"minecraft".equals(channel.namespace())) {
            CLIENT_LOG.debug("CustomPayload received: {} len={}", channel.asString(), data != null ? data.length : -1);
        }
        if (channel.namespace().equals("minecraft") && channel.value().equals("brand")) {
            CACHE.getChunkCache().setServerBrand(data);
            return new ClientboundCustomPayloadPacket(
                packet.getChannel(),
                BrandSerializer.appendBrand(data));
        }
        if (channel.namespace().equals("plasmo") && channel.value().equals("voice/v2")) {
            if (!(CONFIG.server.plasmoVoice.enabled && CONFIG.server.plasmoVoice.udpRelay)) {
                return packet;
            }
            cachePlasmoHandshakePacket(data);
            if (data == null || data.length == 0) {
                return packet;
            }
            int type = data[0] & 0xFF;
            if (type != 0x01) {
                return packet;
            }
            byte[] rewritten = rewritePlasmoVoiceConnectionPacket(data, session);
            if (rewritten != null) {
                if (CONFIG.debug.debugLogs) CLIENT_LOG.info("PV ConnectionPacket rewritten on channel {}", channel.asString());
                ServerSession[] connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    ServerSession connection = connections[i];
                    if (connection.isPlayer() && connection.isConfigured()) {
                        connection.send(new ClientboundCustomPayloadPacket(Key.key("plasmo", "voice/v2"), rewritten));
                    }
                }
                return new ClientboundCustomPayloadPacket(packet.getChannel(), rewritten);
            }
        }
        if (channel.namespace().equals("plasmo") && channel.value().equals("voice/v2/service")) {
            if (CONFIG.debug.debugLogs) CLIENT_LOG.debug("PV Service payload: len={} first={}", data.length, data.length > 0 ? (data[0] & 0xFF) : -1);
        }
        return packet;
    }

    private static void cachePlasmoHandshakePacket(byte[] data) {
        if (data == null || data.length == 0) return;
        synchronized (plasmoHandshakePackets) {
            if (plasmoHandshakePackets.size() >= 128) return;
            byte[] copy = new byte[data.length];
            System.arraycopy(data, 0, copy, 0, data.length);
            plasmoHandshakePackets.add(copy);
        }
    }

    public byte[] getCachedPlasmoVoicePacket() {
        if (lastPlasmoVoicePacketData == null) {
            if (CONFIG.debug.debugLogs) CLIENT_LOG.warn("PV Cached packet requested but is null");
            return null;
        }
        if (CONFIG.debug.debugLogs) CLIENT_LOG.info("PV Cached packet requested, rewriting...");
        return rewritePlasmoVoiceConnectionPacket(lastPlasmoVoicePacketData, Proxy.getInstance().getClient());
    }

    public void sendCachedPlasmoVoiceHandshake(ServerSession connection) {
        byte[][] packets;
        synchronized (plasmoHandshakePackets) {
            if (plasmoHandshakePackets.isEmpty()) return;
            packets = plasmoHandshakePackets.toArray(new byte[0][]);
        }
        for (byte[] original : packets) {
            if (original == null || original.length == 0) continue;
            int type = original[0] & 0xFF;
            byte[] toSend = original;
            if (type == 0x01) {
                byte[] rewritten = rewritePlasmoVoiceConnectionPacket(original, Proxy.getInstance().getClient());
                if (rewritten == null) continue;
                toSend = rewritten;
            }
            connection.send(new ClientboundCustomPayloadPacket(Key.key("plasmo", "voice/v2"), toSend));
        }
    }

    private static byte[] rewritePlasmoVoiceConnectionPacket(byte[] data, ClientSession session) {
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(data);
            int type = in.readByte() & 0xFF;
            if (type != 0x01) return null;
            UUID secret = new UUID(in.readLong(), in.readLong());
            String ip = in.readUTF();
            int port = in.readInt();
            Proxy proxy = Proxy.getInstance();
            String proxyIp = com.zenith.Globals.CONFIG.server.getProxyAddressForTransfer();
            int proxyPort = com.zenith.Globals.CONFIG.server.getProxyPortForTransfer();
            proxyIp = proxyIp.contains(":") ? proxyIp.split(":")[0] : proxyIp;
            String remoteIp = ip;
            if ("0.0.0.0".equals(remoteIp) || remoteIp.isBlank()) {
                remoteIp = session.getHost();
            }
            Proxy.getInstance().getVoiceUdpRelay().updateSecretRemote(secret, new InetSocketAddress(remoteIp, port));
            if (CONFIG.debug.debugLogs) CLIENT_LOG.info("PV ConnectionPacket: secret={}, remote={}:{}, proxy={}:{}", secret, remoteIp, port, proxyIp, proxyPort);
            lastPlasmoVoicePacketData = data;
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeByte(0x01);
            out.writeLong(secret.getMostSignificantBits());
            out.writeLong(secret.getLeastSignificantBits());
            out.writeUTF(proxyIp);
            out.writeInt(proxyPort);
            return out.toByteArray();
        } catch (Exception e) {
            if (CONFIG.debug.debugLogs) CLIENT_LOG.warn("PV ConnectionPacket rewrite failed: {}", e.getMessage());
            return null;
        }
    }
}
