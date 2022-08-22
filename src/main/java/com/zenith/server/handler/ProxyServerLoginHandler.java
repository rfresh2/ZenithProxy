package com.zenith.server.handler;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Session;
import com.zenith.Proxy;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.server.PorkServerConnection;
import com.zenith.server.PorkServerListener;

import static com.zenith.util.Constants.*;

public class ProxyServerLoginHandler implements ServerLoginHandler {
    private final Proxy proxy;

    public ProxyServerLoginHandler(final Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void loggedIn(Session session) {
        PorkServerConnection connection = ((PorkServerListener) this.proxy.getServer().getListeners().stream()
                .filter(PorkServerListener.class::isInstance)
                .findAny().orElseThrow(IllegalStateException::new))
                .getConnections().get(session);

        connection.setPlayer(true);
        SERVER_LOG.info("Player connected: %s", session.getRemoteAddress());

        if (this.proxy.getCurrentPlayer().compareAndSet(null, connection)) {
            // if we don't have a current player, set player
            GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            EVENT_BUS.dispatch(new ProxyClientConnectedEvent(clientGameProfile, true));
            session.send(new ServerJoinGamePacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getPlayerCache().isHardcore(),
                    CACHE.getPlayerCache().getGameMode(),
                    CACHE.getPlayerCache().getDimension(),
                    CACHE.getPlayerCache().getDifficulty(),
                    CACHE.getPlayerCache().getMaxPlayers(),
                    CACHE.getPlayerCache().getWorldType(),
                    CACHE.getPlayerCache().isReducedDebugInfo()
            ));
        } else if (CONFIG.server.allowSpectator) {
            // if we have a current player, allow login but put in spectator
            GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            EVENT_BUS.dispatch(new ProxyClientConnectedEvent(clientGameProfile, false));
            session.send(new ServerJoinGamePacket(
                    connection.getSpectatorEntityId(),
                    CACHE.getPlayerCache().isHardcore(),
                    GameMode.SPECTATOR,
                    CACHE.getPlayerCache().getDimension(),
                    CACHE.getPlayerCache().getDifficulty(),
                    CACHE.getPlayerCache().getMaxPlayers(),
                    CACHE.getPlayerCache().getWorldType(),
                    CACHE.getPlayerCache().isReducedDebugInfo()
            ));
        } else {
            connection.disconnect("§cA client is already connected to this bot!");
        }
    }
}
