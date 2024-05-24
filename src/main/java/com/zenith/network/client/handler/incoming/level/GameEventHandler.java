package com.zenith.network.client.handler.incoming.level;

import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RainStrengthValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.RespawnScreenValue;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.ThunderStrengthValue;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class GameEventHandler implements ClientEventLoopPacketHandler<ClientboundGameEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundGameEventPacket packet, @NonNull ClientSession session) {
        switch (packet.getNotification()) {
            case CHANGE_GAMEMODE -> CACHE.getPlayerCache().setGameMode((GameMode) packet.getValue());
            case START_RAIN -> {
                CACHE.getChunkCache().setRaining(true);
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case STOP_RAIN -> {
                CACHE.getChunkCache().setRaining(false);
                CACHE.getChunkCache().setThunderStrength(0.0f);
                CACHE.getChunkCache().setRainStrength(0.0f);
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case RAIN_STRENGTH -> {
                CACHE.getChunkCache().setRainStrength(((RainStrengthValue) packet.getValue()).getStrength());
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case THUNDER_STRENGTH -> {
                CACHE.getChunkCache().setThunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
                EVENT_BUS.postAsync(WeatherChangeEvent.INSTANCE);
            }
            case ENABLE_RESPAWN_SCREEN -> CACHE.getPlayerCache()
                .setEnableRespawnScreen(packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN);
        }
        return true;
    }
}
