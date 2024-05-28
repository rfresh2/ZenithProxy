package com.zenith.via.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;

import java.util.List;

/**
 * Handler that switches the session protocol state after a packet is serialized but before it gets to via codec.
 *
 * ViaVersion's codec can send packets back to the client, so we need to switch the protocol state
 * before the `packetSent` hook is called.
 *
 * We also can't switch the protocol state in the `packetSending` hook because it's called before MCPL's codec
 * serializes the packet based on the current protocol state.
 */
public class ZViaProtocolStateHandler extends MessageToMessageEncoder<ByteBuf> {
    private final Session session;

    public ZViaProtocolStateHandler(Session session) {
        this.session = session;
    }

    @Override
    protected void encode(final ChannelHandlerContext channelHandlerContext, final ByteBuf in, final List<Object> out) throws Exception {
        in.markReaderIndex();
        try {
            var packetId = session.getCodecHelper().readVarInt(in);
            var packetProtocol = session.getPacketProtocol();
            var serverboundClass = packetProtocol.getServerboundClass(packetId);
            if (serverboundClass == ServerboundLoginAcknowledgedPacket.class
                || serverboundClass == ServerboundConfigurationAcknowledgedPacket.class)
                packetProtocol.setState(ProtocolState.CONFIGURATION);
            else if (serverboundClass == ServerboundFinishConfigurationPacket.class)
                packetProtocol.setState(ProtocolState.GAME);
        } catch (final Throwable e) {
            if (!session.callPacketError(e)) throw e;
        }
        in.resetReaderIndex();
        out.add(in.retain());
    }
}
