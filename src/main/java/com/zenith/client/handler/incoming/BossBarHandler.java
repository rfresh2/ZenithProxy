package com.zenith.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.zenith.client.ClientSession;
import com.zenith.feature.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class BossBarHandler implements HandlerRegistry.AsyncIncomingHandler<ServerBossBarPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerBossBarPacket packet, @NonNull ClientSession session) {
        Consumer<ServerBossBarPacket> consumer = pck -> {
            throw new IllegalStateException();
        };
        switch (packet.getAction())    {
            case ADD:
                consumer = CACHE.getBossBarCache()::add;
                break;
            case REMOVE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = CACHE.getBossBarCache()::remove;
                break;
            case UPDATE_HEALTH:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setHealth(pck.getHealth());
                break;
            case UPDATE_TITLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setTitle(pck.getTitle());
                break;
            case UPDATE_STYLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setColor(pck.getColor()).setDivision(pck.getDivision());
                break;
            case UPDATE_FLAGS:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = pck -> CACHE.getBossBarCache().get(pck).setDarkenSky(pck.getDarkenSky()).setDragonBar(pck.isDragonBar());
                break;
        }
        consumer.accept(packet);
        return true;
    }

    @Override
    public Class<ServerBossBarPacket> getPacketClass() {
        return ServerBossBarPacket.class;
    }
}
