package com.zenith.feature.replay;

import com.zenith.module.impl.ReplayMod;
import com.zenith.network.registry.PacketHandlerCodec;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.util.EnumMap;

public class ReplayModPacketHandlerCodec extends PacketHandlerCodec {
    private final ReplayMod replayMod;

    public ReplayModPacketHandlerCodec(final ReplayMod instance, final int priority, final String id) {
        super(priority, id, new EnumMap<>(ProtocolState.class), (session) -> true);
        this.replayMod = instance;
    }


    @Override
    public <P extends Packet, S extends Session> P handleInbound(@NonNull P packet, @NonNull S session) {
        replayMod.onInboundPacket(packet, session);
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> P handleOutgoing(@NonNull P packet, @NonNull S session) {
        return packet;
    }

    @Override
    public <P extends Packet, S extends Session> void handlePostOutgoing(@NonNull P packet, @NonNull S session) {
        replayMod.onPostOutgoing(packet, session);
    }
}
