package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import com.zenith.event.module.SplashSoundEffectEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncPacketHandler;

import static com.github.steveice10.mc.protocol.data.game.level.sound.BuiltinSound.ENTITY_FISHING_BOBBER_SPLASH;
import static com.zenith.Shared.EVENT_BUS;

public class SoundHandler implements AsyncPacketHandler<ClientboundSoundPacket, ClientSession> {
    @Override
    public boolean applyAsync(ClientboundSoundPacket packet, ClientSession session) {
        if (packet.getSound() == ENTITY_FISHING_BOBBER_SPLASH) EVENT_BUS.postAsync(new SplashSoundEffectEvent(packet));
        return true;
    }
}
