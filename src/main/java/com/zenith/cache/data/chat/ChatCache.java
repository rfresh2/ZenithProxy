package com.zenith.cache.data.chat;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;

import java.util.function.Consumer;

@Data
@Accessors(chain = true)
public class ChatCache implements CachedData {
    protected CommandNode[] commandNodes = new CommandNode[0];
    protected int firstCommandNodeIndex;
    protected volatile long lastChatTimestamp = System.currentTimeMillis();

    // todo: cache chat signing stuff

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer) {
        consumer.accept(new ClientboundCommandsPacket(this.commandNodes, this.firstCommandNodeIndex));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.PROTOCOL_SWITCH || type == CacheResetType.FULL) {
            this.commandNodes = new CommandNode[0];
            this.firstCommandNodeIndex = 0;
            if (type == CacheResetType.FULL) {
                this.lastChatTimestamp = System.currentTimeMillis();
            }
        }
    }

    @Override
    public String getSendingMessage()  {
        return String.format("Sending %s server commands", this.commandNodes.length);
    }

}
