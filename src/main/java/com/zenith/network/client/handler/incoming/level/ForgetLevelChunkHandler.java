package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;

public class ForgetLevelChunkHandler implements ClientEventLoopPacketHandler<ClientboundForgetLevelChunkPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundForgetLevelChunkPacket packet, @NonNull ClientSession session) {
        CACHE.getChunkCache().remove(packet.getX(), packet.getZ());
        SpectatorSync.checkSpectatorPositionOutOfRender(packet.getX(), packet.getZ());
        return true;
    }
}
