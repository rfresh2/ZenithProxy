package com.zenith.cache.data.bossbar;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;

import java.util.UUID;


@Data
@Accessors(chain = true)
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
