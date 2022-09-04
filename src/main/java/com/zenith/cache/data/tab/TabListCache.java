package com.zenith.cache.data.tab;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Getter;
import lombok.NonNull;

import java.util.function.Consumer;


@Getter
public class TabListCache implements CachedData {
    protected TabList tabList = new TabList();

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ServerPlayerListDataPacket(this.tabList.getHeader(), this.tabList.getFooter(), false));
        consumer.accept(new ServerPlayerListEntryPacket(
                PlayerListEntryAction.ADD_PLAYER,
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
