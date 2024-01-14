package com.zenith.feature.queue.mcping;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class Pinger {

    private static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        int k;
        do {
            k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) {
                //throw new RuntimeException("VarInt too big");
                return -1;
            }
        } while ((k & 0x80) == 128);
        return i;
    }

    private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        for (; ; ) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public static String fetchData(InetSocketAddress host, int timeout, int protocolVersion) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeout);
            socket.connect(host, timeout);
            try (DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                 ByteArrayOutputStream b = new ByteArrayOutputStream();
                 DataOutputStream handshake = new DataOutputStream(b)) {
                handshake.writeByte(0);
                writeVarInt(handshake, protocolVersion);
                writeVarInt(handshake, host.getHostString().length());
                handshake.writeBytes(host.getHostString());
                handshake.writeShort(host.getPort());
                writeVarInt(handshake, 1);
                writeVarInt(dataOutputStream, b.size());
                dataOutputStream.write(b.toByteArray());
                dataOutputStream.writeByte(1);
                dataOutputStream.writeByte(0);
                var size = readVarInt(dataInputStream);
                var id = readVarInt(dataInputStream);
                var length = readVarInt(dataInputStream);
                if (size < 0 || id < 0 || length <= 0) return null;
                final byte[] in = new byte[length];
                dataInputStream.readFully(in);
                return new String(in, StandardCharsets.UTF_8);
            }
        }
    }
}
