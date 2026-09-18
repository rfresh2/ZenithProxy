package com.zenith.network.client.handler.postoutgoing;

import com.zenith.event.module.ClientSwingEvent;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPunchPacket;

import static com.zenith.Globals.EVENT_BUS;

public class PostOutgoingSwingHandler implements ClientEventLoopPacketHandler<ServerboundPunchPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundPunchPacket packet, final ClientSession session) {
        SpectatorSync.sendSwing();
        EVENT_BUS.postAsync(ClientSwingEvent.INSTANCE);
        return true;
    }
}
