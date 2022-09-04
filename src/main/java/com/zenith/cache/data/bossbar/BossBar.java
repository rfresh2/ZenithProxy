package com.zenith.cache.data.bossbar;

import com.github.steveice10.mc.protocol.data.game.BossBarAction;
import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;


@Getter
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class BossBar {
    @NonNull
    protected final UUID uuid;

    protected String title;
    protected float health;
    protected BossBarColor color;
    protected BossBarDivision division;
    protected boolean darkenSky;
    protected boolean dragonBar;

    public ServerBossBarPacket toMCProtocolLibPacket()  {
        return new ServerBossBarPacket(
                this.uuid,
                BossBarAction.ADD,
                this.title,
                this.health,
                this.color,
                this.division,
                this.darkenSky,
                this.dragonBar,
                false
        );
    }
}
