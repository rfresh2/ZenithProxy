package com.zenith.network.registry;

import com.zenith.util.SortedFastArrayList;
import lombok.Data;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    private final SortedFastArrayList<PacketHandlerCodec> codecs = new SortedFastArrayList<>(
        PacketHandlerCodec.class,
        Comparator.comparingInt(PacketHandlerCodec::getPriority).reversed());
    private final String id;

    synchronized public void register(final PacketHandlerCodec codec) {
        if (getCodec(codec.getId()) != null) return;
        codecs.add(codec);
        DEFAULT_LOG.debug("[{}] Registered codec: {} with priority: {}, pipeline: {}", id, codec.getId(), codec.getPriority(), getCodecIds());
    }

    synchronized public void unregister(final PacketHandlerCodec codec) {
        codecs.remove(codec);
        DEFAULT_LOG.debug("[{}] Unregistered codec: {}, pipeline: {}", id, codec.getId(), getCodecIds());
    }

    public PacketHandlerCodec getCodec(final String id) {
        var codecs = this.codecs.getArray();
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
        var codecs = this.codecs.getArray();
        var ids = new ArrayList<String>(codecs.length);
        for (int i = 0; i < codecs.length; i++) {
            ids.add(codecs[i].getId());
        }
        return ids;
    }

    public <P extends Packet, S extends Session> P handleInbound(@Nullable P packet, @NonNull S session) {
        if (packet == null) return null;
        P p = packet;
        var codecs = this.codecs.getArray();
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
        var codecs = this.codecs.getArray();
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
        var codecs = this.codecs.getArray();
        for (int i = codecs.length - 1; i >= 0; i--) {
            var codec = codecs[i];
            if (codec.getActivePredicate().test(session))
                codec.handlePostOutgoing(packet, session);
        }
    }
}
