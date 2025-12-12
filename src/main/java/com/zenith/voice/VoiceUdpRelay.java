package com.zenith.voice;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceUdpRelay {
    private static final int MAGIC = 0x4e9004e9;
    private final Map<UUID, InetSocketAddress> secretToClient = new ConcurrentHashMap<>();
    private final Map<UUID, InetSocketAddress> secretToRemote = new ConcurrentHashMap<>();
    private volatile boolean running;
    private DatagramSocket socket;
    private Thread thread;

    public synchronized void start(String bindIp, int port) {
        if (running) return;
        try {
            socket = new DatagramSocket(new InetSocketAddress(bindIp, port));
            running = true;
            thread = Thread.ofVirtual().name("Voice UDP Relay").start(this::runLoop);
            if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.info("Voice UDP Relay listening on {}:{}", bindIp, port);
        } catch (Exception e) {
            running = false;
            if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.warn("Voice UDP Relay start failed: {}", e.getMessage());
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (socket != null) socket.close();
            if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.info("Voice UDP Relay stopped");
        } catch (Exception ignored) {}
    }

    public void updateSecretRemote(UUID secret, InetSocketAddress remote) {
        if (secret == null || remote == null) return;
        secretToRemote.put(secret, remote);
        if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.info("PV secret mapped: {} -> {}:{}", secret, remote.getHostString(), remote.getPort());
    }

    private void runLoop() {
        byte[] buf = new byte[65535];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                byte[] data = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
                ByteArrayDataInput in = ByteStreams.newDataInput(data);
                int magic;
                try {
                    magic = in.readInt();
                } catch (Exception e) {
                    continue;
                }
                if (magic != MAGIC) continue;
                int type = in.readByte() & 0xFF;
                UUID secret = new UUID(in.readLong(), in.readLong());
                in.readLong();
                SocketAddress src = new InetSocketAddress(packet.getAddress(), packet.getPort());
                InetSocketAddress remote = secretToRemote.get(secret);
                InetSocketAddress client = secretToClient.get(secret);
                boolean fromRemote = remote != null && src instanceof InetSocketAddress s && s.getAddress().equals(remote.getAddress()) && s.getPort() == remote.getPort();
                if (!fromRemote) {
                    secretToClient.put(secret, (InetSocketAddress) src);
                    if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.debug("PV UDP from client {}:{} secret={}", ((InetSocketAddress) src).getHostString(), ((InetSocketAddress) src).getPort(), secret);
                    if (remote != null) {
                        DatagramPacket forward = new DatagramPacket(data, data.length, remote);
                        socket.send(forward);
                        if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.debug("PV UDP forwarded to remote {}:{} secret={}", remote.getHostString(), remote.getPort(), secret);
                    }
                } else {
                    if (client != null) {
                        DatagramPacket back = new DatagramPacket(data, data.length, client);
                        socket.send(back);
                        if (com.zenith.Globals.CONFIG.debug.debugLogs) com.zenith.Globals.SERVER_LOG.debug("PV UDP forwarded back to client {}:{} secret={}", client.getHostString(), client.getPort(), secret);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
