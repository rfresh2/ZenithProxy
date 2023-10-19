package com.zenith.event.module;

public record SplashSoundEffectEvent(
    com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket packet) { }
