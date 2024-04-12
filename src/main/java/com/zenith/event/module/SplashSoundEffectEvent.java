package com.zenith.event.module;

public record SplashSoundEffectEvent(
    org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket packet) { }
