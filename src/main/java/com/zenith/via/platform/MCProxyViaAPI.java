package com.zenith.via.platform;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.viaversion.viaversion.ViaAPIBase;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;

public class MCProxyViaAPI extends ViaAPIBase<MinecraftProtocol> {
    @Override
    public int getPlayerVersion(MinecraftProtocol player) {
        return ProtocolVersion.v1_12_2.getVersion();
    }

    @Override
    public void sendRawPacket(MinecraftProtocol player, ByteBuf packet) {
        sendRawPacket(player.getProfile().getId(), packet);
    }
}
