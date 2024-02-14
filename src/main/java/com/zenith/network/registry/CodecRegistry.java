package com.zenith.network.registry;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.zenith.Shared.DEFAULT_LOG;

@Data
public class CodecRegistry {
    /**
     * Codecs are invoked similar to the netty pipeline
     *
     * For inbound packets, higher priority codecs are invoked first
     * For outbound packets, lower priority codecs are invoked first
     */

    private PacketHandlerCodec[] codecs = new PacketHandlerCodec[0];
    private final String id;

    public void register(final PacketHandlerCodec codec) {
        if (getCodec(codec.getId()) != null) return;
        var newCodecs = new PacketHandlerCodec[codecs.length + 1];
        System.arraycopy(codecs, 0, newCodecs, 0, codecs.length);
        newCodecs[codecs.length] = codec;
        Arrays.sort(newCodecs, Comparator.comparingInt(PacketHandlerCodec::getPriority));
        this.codecs = newCodecs;
        DEFAULT_LOG.debug("[{}] Registered codec: {} with priority: {}, pipeline: {}", id, codec.getId(), codec.getPriority(), getCodecIds());
    }

    public void unregister(final PacketHandlerCodec codec) {
        int removeIndex = -1;
        for (int i = 0; i < codecs.length; i++) {
            if (codecs[i] == codec) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex == -1) return;
        var newCodecs = new PacketHandlerCodec[codecs.length - 1];
        System.arraycopy(codecs, 0, newCodecs, 0, removeIndex);
        System.arraycopy(codecs, removeIndex + 1, newCodecs, removeIndex, codecs.length - removeIndex - 1);
        this.codecs = newCodecs;
        DEFAULT_LOG.debug("[{}] Unregistered codec: {}, pipeline: {}", id, codec.getId(), getCodecIds());
    }

    public void unregister(final String id) {
        for (int i = 0; i < codecs.length; i++) {
            if (codecs[i].getId().equals(id)) {
                unregister(codecs[i]);
                return;
            }
        }
    }

    public PacketHandlerCodec getCodec(final String id) {
        for (int i = 0; i < codecs.length; i++) {
            var codec = codecs[i];
            if (codec.getId().equals(id)) {
                return codec;
            }
        }
        return null;
    }

    // in-order based on priority
    public List<String> getCodecIds() {
        var ids = new ArrayList<String>(codecs.length);
        for (int i = 0; i < codecs.length; i++) {
            ids.add(codecs[i].getId());
        }
        return ids;
    }

    public <P extends Packet, S extends Session> P handleInbound(@Nullable P packet, @NonNull S session) {
        if (packet == null) return null;
        P p = packet;
        for (int i = 0; i < codecs.length; i++) {
            if (p == null) break;
            var codec = codecs[i];
            if (codec.getActivePredicate().test(session))
                p = codec.handleInbound(p, session);
        }
        return p;
    }

    public <P extends Packet, S extends Session> P handleOutgoing(@Nullable P packet, @NonNull S session) {
        if (packet == null) return null;
        P p = packet;
        for (int i = codecs.length - 1; i >= 0; i--) {
            if (p == null) break;
            var codec = codecs[i];
            if (codec.getActivePredicate().test(session))
                p = codec.handleOutgoing(p, session);
        }
        return p;
    }

    public <P extends Packet, S extends Session> void handlePostOutgoing(@Nullable P packet, @NonNull S session) {
        if (packet == null) return;
        for (int i = codecs.length - 1; i >= 0; i--) {
            var codec = codecs[i];
            if (codec.getActivePredicate().test(session))
                codecs[i].handlePostOutgoing(packet, session);
        }
    }
}
