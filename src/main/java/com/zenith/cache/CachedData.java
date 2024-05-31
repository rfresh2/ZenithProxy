package com.zenith.cache;

import lombok.NonNull;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.function.Consumer;


public interface CachedData {
    void getPackets(@NonNull Consumer<Packet> consumer);

    void reset(CacheResetType type);

    default String getSendingMessage()  {
        return null;
    }
}
