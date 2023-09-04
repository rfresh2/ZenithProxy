package com.zenith.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Getter;
import lombok.NonNull;

import java.util.EnumSet;
import java.util.function.Consumer;


@Getter
public class TabListCache implements CachedData {
    protected TabList tabList = new TabList();

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundTabListPacket(this.tabList.getHeader(), this.tabList.getFooter()));
        consumer.accept(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(PlayerListEntryAction.ADD_PLAYER, PlayerListEntryAction.UPDATE_LISTED),
            this.tabList.getEntries().stream().map(PlayerEntry::toMCProtocolLibEntry).toArray(PlayerListEntry[]::new)
        ));
    }

    @Override
    public void reset(boolean full) {
        if (full) {
            this.tabList = new TabList();
        }
    }

    @Override
    public String getSendingMessage() {
        return "Sending tab list";
    }
}
