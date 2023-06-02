package com.zenith.via.platform;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.viaversion.viaversion.ViaAPIBase;
import io.netty.buffer.ByteBuf;

public class MCProxyViaAPI extends ViaAPIBase<MinecraftProtocol> {
    @Override
    public int getPlayerVersion(MinecraftProtocol player) {
        return 0;
    }

    @Override
    public void sendRawPacket(MinecraftProtocol player, ByteBuf packet) {

    }
}
