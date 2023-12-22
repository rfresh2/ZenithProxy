package com.zenith.via.handler;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.codec.PacketCodecHelper;
import com.github.steveice10.packetlib.codec.PacketDefinition;
import com.github.steveice10.packetlib.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

public class ZViaClientCodecHandler extends ByteToMessageCodec<Packet> {
    private final Session session;
    public ZViaClientCodecHandler(Session session) {
        this.session = session;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf buf) throws Exception {
        int initial = buf.writerIndex();

        MinecraftProtocol packetProtocol = this.session.getPacketProtocol();
        PacketCodecHelper codecHelper = this.session.getCodecHelper();
        try {
            int packetId = packetProtocol.getServerboundId(packet);
            PacketDefinition definition = packetProtocol.getServerboundDefinition(packetId);

            packetProtocol.getPacketHeader().writePacketId(buf, codecHelper, packetId);
            definition.getSerializer().serialize(buf, codecHelper, packet);
            // need to update protocol state here instead of PostOutgoing handlers due to via
            if (packet instanceof ServerboundLoginAcknowledgedPacket) {
                packetProtocol.setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
            } else if (packet instanceof ServerboundFinishConfigurationPacket) {
                packetProtocol.setState(ProtocolState.GAME); // CONFIGURATION -> GAME
            } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
                packetProtocol.setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
            }
        } catch (Throwable t) {
            // Reset writer index to make sure incomplete data is not written out.
            buf.writerIndex(initial);
            if (!this.session.callPacketError(t)) {
                throw t;
            }
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        int initial = buf.readerIndex();

        MinecraftProtocol packetProtocol = this.session.getPacketProtocol();
        PacketCodecHelper codecHelper = this.session.getCodecHelper();
        try {
            int id = packetProtocol.getPacketHeader().readPacketId(buf, codecHelper);
            if (id == -1) {
                buf.readerIndex(initial);
                return;
            }

            Packet packet = packetProtocol.createClientboundPacket(id, buf, codecHelper);

            if (buf.readableBytes() > 0) {
                throw new IllegalStateException("Packet \"" + packet.getClass().getSimpleName() + "\" not fully read.");
            }

            out.add(packet);
        } catch (Throwable t) {
            // Advance buffer to end to make sure remaining data in this packet is skipped.
            buf.readerIndex(buf.readerIndex() + buf.readableBytes());
            if (!this.session.callPacketError(t)) {
                throw t;
            }
        }
    }
}
