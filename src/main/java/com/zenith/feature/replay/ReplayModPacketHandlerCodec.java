package com.zenith.feature.replay;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.module.impl.ReplayMod;
import com.zenith.network.registry.PacketHandlerCodec;
import lombok.NonNull;

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
