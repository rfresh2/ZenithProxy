package com.zenith.cache.data.scoreboard;

import com.github.steveice10.mc.protocol.data.game.chat.numbers.NumberFormat;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import lombok.Data;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

@Data
public class Score {
    @NonNull
    protected final String owner;

    protected int value;
    protected Component display;
    protected NumberFormat numberFormat;

    public Score(ClientboundSetScorePacket packet) {
        this.owner = packet.getOwner();
        this.value = packet.getValue();
        this.display = packet.getDisplay();
        this.numberFormat = packet.getNumberFormat();
    }

    public ClientboundSetScorePacket toPacket(String objective) {
        return new ClientboundSetScorePacket(this.owner, objective, this.value)
                // full constructor is private for some reason?
                .withDisplay(this.display)
                .withNumberFormat(this.numberFormat);
    }
}
