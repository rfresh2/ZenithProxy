package com.zenith.server.handler;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Session;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxySpectatorConnectedEvent;
import com.zenith.server.ProxyServerListener;
import com.zenith.server.ServerConnection;
import com.zenith.util.Wait;

import static com.zenith.util.Constants.*;
import static java.util.Objects.nonNull;

public class ProxyServerLoginHandler implements ServerLoginHandler {
    private final Proxy proxy;

    public ProxyServerLoginHandler(final Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void loggedIn(Session session) {
        ServerConnection connection = ((ProxyServerListener) this.proxy.getServer().getListeners().stream()
                .filter(ProxyServerListener.class::isInstance)
                .findAny().orElseThrow(IllegalStateException::new))
                .getConnections().get(session);
        SERVER_LOG.info("Player connected: {}", session.getRemoteAddress());
        if (!Wait.waitUntilCondition(() -> Proxy.getInstance().isConnected()
                        && CACHE.getPlayerCache().getEntityId() != -1
                        && nonNull(CACHE.getProfileCache().getProfile())
                        && nonNull(CACHE.getPlayerCache().getGameMode())
                        && nonNull(CACHE.getPlayerCache().getDifficulty())
                        && nonNull(CACHE.getPlayerCache().getWorldType()),
                10)) {
            session.disconnect("Client login timed out.");
            return;
        }
        connection.setPlayer(true);
        if (!connection.isOnlySpectator() && this.proxy.getCurrentPlayer().compareAndSet(null, connection)) {
            // if we don't have a current player, set player
            connection.setSpectator(false);
            GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            EVENT_BUS.dispatch(new ProxyClientConnectedEvent(clientGameProfile));
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
            PlayerCache.syncInv();
        } else {
            if (nonNull(this.proxy.getCurrentPlayer().get())) {
                // if we have a current player, allow login but put in spectator
                connection.setSpectator(true);
                GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                EVENT_BUS.dispatch(new ProxySpectatorConnectedEvent(clientGameProfile));
                session.send(new ServerJoinGamePacket(
                        connection.getSpectatorSelfEntityId(),
                        CACHE.getPlayerCache().isHardcore(),
                        GameMode.SPECTATOR,
                        CACHE.getPlayerCache().getDimension(),
                        CACHE.getPlayerCache().getDifficulty(),
                        CACHE.getPlayerCache().getMaxPlayers(),
                        CACHE.getPlayerCache().getWorldType(),
                        CACHE.getPlayerCache().isReducedDebugInfo()
                ));
            } else {
                // can probably make this state work with some more work but im just gonna block it for now
                connection.disconnect("A player must be connected in order to spectate!");
            }
        }
    }
}
