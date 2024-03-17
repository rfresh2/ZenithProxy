package com.zenith.network.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.zenith.event.module.ClientSwingEvent;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.zenith.Shared.EVENT_BUS;

public class PostOutgoingSwingHandler implements AsyncPacketHandler<ServerboundSwingPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundSwingPacket packet, final ClientSession session) {
        SpectatorSync.sendSwing();
        EVENT_BUS.postAsync(ClientSwingEvent.INSTANCE);
        return true;
    }
}
