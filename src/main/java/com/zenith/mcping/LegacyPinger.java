package com.zenith.mcping;

import com.zenith.mcping.data.FinalResponse;
import com.zenith.mcping.rawData.Players;
import com.zenith.mcping.rawData.Version;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.zenith.mcping.MCPing.PROTOCOL_VERSION_DISCOVERY;

/**
 * Pinger for 1.6 protocol
 * https://wiki.vg/Server_List_Ping#Client_to_server
 */
public class LegacyPinger {
    private InetSocketAddress host;
    private int timeout;
    private int protocolVersion = PROTOCOL_VERSION_DISCOVERY;

    void setAddress(InetSocketAddress host) {
        this.host = host;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public LegacyPingResponse ping() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(this.timeout);
        socket.connect(this.host, this.timeout);
        sendPing(new DataOutputStream(socket.getOutputStream()));
        return readResponse(new DataInputStream(socket.getInputStream()));
    }

    private void sendPing(DataOutputStream dataOutputStream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(bytes);
        handshake.writeByte(0xFE);
        handshake.writeByte(0x01);
        handshake.writeByte(0xFA);
        byte[] hostNameBytes = host.getHostName().getBytes(StandardCharsets.UTF_16BE);
        String hostStr = "MC|" + host.getHostName();
        byte[] hostStrBytes = hostStr.getBytes(StandardCharsets.UTF_16BE);
        handshake.writeShort(hostStrBytes.length);
        for (byte aByte : hostStrBytes) {
            handshake.writeByte(aByte);
        }
        handshake.write(7 + hostStrBytes.length);
        handshake.write(protocolVersion);
        handshake.writeShort(hostNameBytes.length);
        for (byte aByte : hostNameBytes) {
            handshake.writeByte(aByte);
        }
        handshake.writeInt(host.getPort());
        dataOutputStream.write(bytes.toByteArray());
    }

    private LegacyPingResponse readResponse(DataInputStream dataInputStream) throws IOException {
        dataInputStream.readByte(); // Single data identifier
        dataInputStream.readShort();
        byte[] bytes = readNBytes(dataInputStream, dataInputStream.available());
        String str = new String(bytes, StandardCharsets.UTF_16BE);
        String[] split = str.split("\\0000");
        if (split.length != 6) throw new RuntimeException("Bad response: " + Arrays.toString(split));
        return new LegacyPingResponse(Integer.parseInt(split[1]), split[2], split[3], Integer.parseInt(split[4]), Integer.parseInt(split[5]));
    }

    /**
     * Only present in JDK11+, copying here for use
     */
    public byte[] readNBytes(final DataInputStream dataInputStream, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, 8192)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = dataInputStream.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (2048 - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

//    public static void main(String[] args) throws IOException {
//        int protocol = ProtocolVersions.findProtocolVersion("1.8").get();
//        LegacyPinger pinger = new LegacyPinger();
//        pinger.setAddress(new InetSocketAddress("78.34.48.126",25565));
//        pinger.setTimeout(5000);
//        pinger.setProtocolVersion(protocol);
//        System.out.println(pinger.ping());
//    }

    public static final class LegacyPingResponse {
        public final int protocolVersion;
        public final String serverVersion;
        public final String motd;
        public final int players;
        public final int maxPlayers;

        public LegacyPingResponse(int protocolVersion, String serverVersion, String motd, int players, int maxPlayers) {
            this.protocolVersion = protocolVersion;
            this.serverVersion = serverVersion;
            this.motd = motd;
            this.players = players;
            this.maxPlayers = maxPlayers;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LegacyPingResponse{");
            sb.append("protocolVersion=").append(protocolVersion);
            sb.append(", serverVersion='").append(serverVersion).append('\'');
            sb.append(", motd='").append(motd).append('\'');
            sb.append(", players=").append(players);
            sb.append(", maxPlayers=").append(maxPlayers);
            sb.append('}');
            return sb.toString();
        }

        public FinalResponse toFinalResponse() {
            Players players = new Players();
            players.setOnline(this.players);
            players.setMax(this.maxPlayers);
            Version version = new Version();
            version.setProtocol(this.protocolVersion);
            version.setName(this.serverVersion);
            return new FinalResponse(players, version, null, motd);
        }
    }
}
