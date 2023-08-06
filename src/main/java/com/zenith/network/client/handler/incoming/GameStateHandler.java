package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.data.game.world.notify.RainStrengthValue;
import com.github.steveice10.mc.protocol.data.game.world.notify.ThunderStrengthValue;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.zenith.event.module.WeatherChangeEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.EVENT_BUS;

public class GameStateHandler implements AsyncIncomingHandler<ServerNotifyClientPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ServerNotifyClientPacket packet, @NonNull ClientSession session) {
        if (packet.getNotification() == ClientNotification.CHANGE_GAMEMODE) {
            CACHE.getPlayerCache().setGameMode((GameMode) packet.getValue());
        } else if (packet.getNotification() == ClientNotification.START_RAIN) {
            CACHE.getChunkCache().setRaining(true);
            EVENT_BUS.dispatch(new WeatherChangeEvent());
        } else if (packet.getNotification() == ClientNotification.STOP_RAIN) {
            CACHE.getChunkCache().setRaining(false);
            CACHE.getChunkCache().setThunderStrength(0.0f);
            CACHE.getChunkCache().setRainStrength(0.0f);
            EVENT_BUS.dispatch(new WeatherChangeEvent());
        } else if (packet.getNotification() == ClientNotification.RAIN_STRENGTH) {
            CACHE.getChunkCache().setRainStrength(((RainStrengthValue) packet.getValue()).getStrength());
            EVENT_BUS.dispatch(new WeatherChangeEvent());
        } else if (packet.getNotification() == ClientNotification.THUNDER_STRENGTH) {
            CACHE.getChunkCache().setThunderStrength(((ThunderStrengthValue) packet.getValue()).getStrength());
            EVENT_BUS.dispatch(new WeatherChangeEvent());
        }
        return true;
    }

    @Override
    public Class<ServerNotifyClientPacket> getPacketClass() {
        return ServerNotifyClientPacket.class;
    }
}
