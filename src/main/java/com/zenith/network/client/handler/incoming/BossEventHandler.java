package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import java.util.function.Consumer;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.isNull;

public class BossEventHandler implements AsyncIncomingHandler<ClientboundBossEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundBossEventPacket packet, @NonNull ClientSession session) {
        Consumer<ClientboundBossEventPacket> consumer = p -> {
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
                consumer = p -> CACHE.getBossBarCache().get(p).setHealth(p.getHealth());
                break;
            case UPDATE_TITLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = p -> CACHE.getBossBarCache().get(p).setTitle(p.getTitle());
                break;
            case UPDATE_STYLE:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = p -> CACHE.getBossBarCache().get(p).setColor(p.getColor()).setDivision(p.getDivision());
                break;
            case UPDATE_FLAGS:
                if (isNull(CACHE.getBossBarCache().get(packet))) return false;
                consumer = p -> CACHE.getBossBarCache().get(p).setDarkenSky(p.isDarkenSky()).setPlayEndMusic(p.isPlayEndMusic());
                break;
        }
        consumer.accept(packet);
        return true;
    }
}
