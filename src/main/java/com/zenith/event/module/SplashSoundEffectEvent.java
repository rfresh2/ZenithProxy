package com.zenith.event.module;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;

public record SplashSoundEffectEvent(ClientboundSoundPacket packet) { }
