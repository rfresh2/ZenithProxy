package com.zenith.network.server.handler;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.proxy.PlayerLoginEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxySpectatorConnectedEvent;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Wait;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.ServerLoginHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class ProxyServerLoginHandler implements ServerLoginHandler {
    @Override
    public void loggedIn(Session session) {
        final GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        SERVER_LOG.info("Player connected: UUID: {}, Username: {}, Address: {}", clientGameProfile.getId(), clientGameProfile.getName(), session.getRemoteAddress());
        EXECUTOR.execute(() -> finishLogin((ServerConnection) session));
    }

    private void finishLogin(ServerConnection connection) {
        final GameProfile clientGameProfile = connection.getFlag(MinecraftConstants.PROFILE_KEY);
        if (!Wait.waitUntil(() -> Proxy.getInstance().isConnected()
                                && (Proxy.getInstance().getOnlineTimeSeconds() > 3 || Proxy.getInstance().isInQueue())
                                && CACHE.getPlayerCache().getEntityId() != -1
                                && nonNull(CACHE.getProfileCache().getProfile())
                                && nonNull(CACHE.getPlayerCache().getGameMode())
                                && nonNull(CACHE.getChunkCache().getCurrentDimension())
                                && nonNull(CACHE.getChunkCache().getWorldName())
                                && nonNull(CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId()))
                                && connection.isWhitelistChecked(),
                            20)) {
            connection.disconnect("Client login timed out.");
            return;
        }
        // avoid race condition if player disconnects sometime during our wait
        if (!connection.isConnected()) return;
        connection.setPlayer(true);
        EVENT_BUS.post(new PlayerLoginEvent(connection));
        if (connection.isSpectator()) {
            EVENT_BUS.post(new ProxySpectatorConnectedEvent(clientGameProfile));
            connection.send(new ClientboundLoginPacket(
                connection.getSpectatorSelfEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                CACHE.getChunkCache().getDimensionRegistry().keySet().toArray(new String[0]),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                CACHE.getPlayerCache().isReducedDebugInfo(),
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getPlayerCache().isDoLimitedCrafting(),
                new PlayerSpawnInfo(
                    CACHE.getChunkCache().getCurrentDimension().name(),
                    CACHE.getChunkCache().getWorldName(),
                    CACHE.getChunkCache().getHashedSeed(),
                    GameMode.SPECTATOR,
                    GameMode.SPECTATOR,
                    CACHE.getChunkCache().isDebug(),
                    CACHE.getChunkCache().isFlat(),
                    CACHE.getPlayerCache().getLastDeathPos(),
                    CACHE.getPlayerCache().getPortalCooldown()
                )
            ));
        } else {
            EVENT_BUS.post(new ProxyClientConnectedEvent(clientGameProfile));
            connection.send(new ClientboundLoginPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                CACHE.getChunkCache().getDimensionRegistry().keySet().toArray(new String[0]),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                CACHE.getPlayerCache().isReducedDebugInfo(),
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getPlayerCache().isDoLimitedCrafting(),
                new PlayerSpawnInfo(
                    CACHE.getChunkCache().getCurrentDimension().name(),
                    CACHE.getChunkCache().getWorldName(),
                    CACHE.getChunkCache().getHashedSeed(),
                    CACHE.getPlayerCache().getGameMode(),
                    CACHE.getPlayerCache().getGameMode(),
                    CACHE.getChunkCache().isDebug(),
                    CACHE.getChunkCache().isFlat(),
                    CACHE.getPlayerCache().getLastDeathPos(),
                    CACHE.getPlayerCache().getPortalCooldown()
                )
            ));
            if (!Proxy.getInstance().isInQueue()) { PlayerCache.sync(); }
        }
        CustomServerInfoBuilder serverInfoBuilder = (CustomServerInfoBuilder) Proxy.getInstance().getServer().getGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
        connection.send(new ClientboundServerDataPacket(
            serverInfoBuilder.getMotd(),
            Proxy.getInstance().getServerIcon(),
            false
        ));
        connection.setConfigured(true);
    }
}
