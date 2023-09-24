package com.zenith.network.client.handler.incoming.level;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.level.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.RespawnScreenValue;
import com.github.steveice10.mc.protocol.data.game.level.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class GameEventHandler implements AsyncIncomingHandler<ClientboundGameEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundGameEventPacket packet, @NonNull ClientSession session) {
        if (packet.getNotification() == GameEvent.CHANGE_GAMEMODE) {
            CACHE.getPlayerCache().setGameMode((GameMode) packet.getValue());
        } else if (packet.getNotification() == GameEvent.START_RAIN) {
            CACHE.getChunkCache().setRaining(true);
            EVENT_BUS.postAsync(new WeatherChangeEvent());
        } else if (packet.getNotification() == GameEvent.STOP_RAIN) {
            CACHE.getChunkCache().setRaining(false);
            CACHE.getChunkCache().setThunderStrength(0.0f);
            CACHE.getChunkCache().setRainStrength(0.0f);
            EVENT_BUS.postAsync(new WeatherChangeEvent());
        } else if (packet.getNotification() == GameEvent.RAIN_STRENGTH) {
            CACHE.getChunkCache().setRainStrength(((RainStrengthValue) packet.getValue()).getStrength());
            EVENT_BUS.postAsync(new WeatherChangeEvent());
        } else if (packet.getNotification() == GameEvent.THUNDER_STRENGTH) {
            CACHE.getChunkCache().setThunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
            EVENT_BUS.postAsync(new WeatherChangeEvent());
        } else if (packet.getNotification() == GameEvent.ENABLE_RESPAWN_SCREEN) {
            CACHE.getPlayerCache().setEnableRespawnScreen(packet.getValue() == RespawnScreenValue.ENABLE_RESPAWN_SCREEN);
        }
        return true;
    }
}
