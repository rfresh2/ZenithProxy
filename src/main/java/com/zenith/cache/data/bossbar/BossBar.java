package com.zenith.cache.data.bossbar;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;

import java.util.UUID;


@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class BossBar {
    @NonNull
    protected final UUID uuid;

    protected Component title;
    protected float health;
    protected BossBarColor color;
    protected BossBarDivision division;
    protected boolean darkenSky;
    protected boolean playEndMusic;

    public ClientboundBossEventPacket toMCProtocolLibPacket()  {
        return new ClientboundBossEventPacket(
                this.uuid,
                this.title,
                this.health,
                this.color,
                this.division,
                this.darkenSky,
                this.playEndMusic,
                false
        );
    }
}
