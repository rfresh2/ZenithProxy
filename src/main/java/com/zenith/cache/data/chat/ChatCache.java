package com.zenith.cache.data.chat;

import com.github.steveice10.mc.protocol.data.game.command.CommandNode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class ChatCache implements CachedData {
    protected CommandNode[] commandNodes = new CommandNode[0];
    protected int firstCommandNodeIndex;
    protected long lastChatTimestamp = 0L;

    // todo: cache chat signing stuff

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer) {
        consumer.accept(new ClientboundCommandsPacket(this.commandNodes, this.firstCommandNodeIndex));
    }

    @Override
    public void reset(final boolean full) {
        if (full) {
            this.commandNodes = new CommandNode[0];
            this.firstCommandNodeIndex = 0;
            this.lastChatTimestamp = 0L;
        }
    }
}
