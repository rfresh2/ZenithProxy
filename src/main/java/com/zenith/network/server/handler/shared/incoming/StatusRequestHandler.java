package com.zenith.network.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

public class StatusRequestHandler implements PacketHandler<ServerboundStatusRequestPacket, ServerConnection> {
    @Override
    public ServerboundStatusRequestPacket apply(final ServerboundStatusRequestPacket packet, final ServerConnection session) {
        ServerInfoBuilder builder = session.getFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
        if (builder == null) {
            builder = $ -> new ServerStatusInfo(
                new VersionInfo(session.getPacketProtocol().getCodec().getMinecraftVersion(), session.getPacketProtocol().getCodec().getProtocolVersion()),
                new PlayerInfo(0, 20, new ArrayList<>()),
                Component.text("A Minecraft Server"),
                null,
                false
            );
        }

        ServerStatusInfo info = builder.buildInfo(session);
        if (info == null) session.disconnect("bye");
        else session.send(new ClientboundStatusResponsePacket(info));
        return null;
    }
}
