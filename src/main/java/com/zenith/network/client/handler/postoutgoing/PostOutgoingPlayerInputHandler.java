package com.zenith.network.client.handler.postoutgoing;

import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.PostOutgoingPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;

import static com.zenith.Globals.CACHE;

public class PostOutgoingPlayerInputHandler implements PostOutgoingPacketHandler<ServerboundPlayerInputPacket, ClientSession> {
    @Override
    public void accept(final ServerboundPlayerInputPacket packet, final ClientSession session) {
        var sneak = packet.isShift();
        var cacheSneak = CACHE.getPlayerCache().isSneaking();
        if (sneak != cacheSneak) {
            CACHE.getPlayerCache().setSneaking(sneak);
            SpectatorSync.sendPlayerPose();
        }
    }
}
