package com.zenith.cache.data.scoreboard;

import lombok.Data;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;

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
        return new ClientboundSetScorePacket(this.owner, objective, this.value, this.display, this.numberFormat);
    }
}
