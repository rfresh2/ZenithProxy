package com.zenith.cache;

import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;

import java.util.function.Consumer;


public interface CachedData {
    void getPackets(@NonNull Consumer<Packet> consumer);

    void reset(boolean full);

    default String getSendingMessage()  {
        return null;
    }
}
