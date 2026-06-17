package com.zenith.network.server.handler;

import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.player.PlayerConnectedEvent;
import com.zenith.event.player.PlayerLoginEvent;
import com.zenith.event.player.SpectatorConnectedEvent;
import com.zenith.feature.player.World;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.ZenithServerInfoBuilder;
import com.zenith.util.Wait;
import com.zenith.util.struct.Maps;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.ServerLink;
import org.geysermc.mcprotocollib.protocol.data.game.ServerLinkType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerSpawnInfo;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomReportDetailsPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundServerLinksPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;

import static com.zenith.Globals.*;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

public class ProxyServerLoginHandler {
    public static final ProxyServerLoginHandler INSTANCE = new ProxyServerLoginHandler();

    public void loggedIn(ServerSession session) {
        final GameProfile clientGameProfile = session.getProfileCache().getProfile();
        if (clientGameProfile == null) {
            session.disconnect("Failed to retrieve profile.");
            return;
        }
        SERVER_LOG.info("Player connected: UUID: {}, Username: {}, MC: {} Address: {}", clientGameProfile.getId(), clientGameProfile.getName(), session.getMCVersion(), session.getRemoteAddress());
        EXECUTOR.execute(() -> finishLogin(session));
    }

    private void finishLogin(ServerSession connection) {
        final GameProfile clientGameProfile = connection.getProfileCache().getProfile();
        if (clientGameProfile == null) {
            connection.disconnect("Failed to retrieve profile.");
            return;
        }
        if (!Wait.waitUntil(() -> Proxy.getInstance().isConnected()
                                && (Proxy.getInstance().getOnlineTimeSeconds() > 1 || Proxy.getInstance().isInQueue())
                                && CACHE.getPlayerCache().getEntityId() != -1
                                && nonNull(CACHE.getProfileCache().getProfile())
                                && nonNull(CACHE.getPlayerCache().getGameMode())
                                && nonNull(CACHE.getChunkCache().getCurrentDimension())
                                && nonNull(CACHE.getChunkCache().getWorldName())
                                && nonNull(CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId()))
                                && connection.isWhitelistChecked()
                                && CACHE.getPlayerCache().getTeleportQueue().isEmpty(),
                            20)) {
            connection.disconnect("Client login timed out.");
            return;
        }
        // avoid race condition if player disconnects sometime during our wait
        if (!connection.isConnected()) return;
        connection.setPlayer(true);
        EVENT_BUS.post(new PlayerLoginEvent.Pre(connection));
        if (!connection.isConnected()) return;
        if (connection.isSpectator()) {
            EVENT_BUS.post(new SpectatorConnectedEvent(connection, clientGameProfile));
            connection.send(new ClientboundLoginPacket(
                connection.getSpectatorSelfEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                CACHE.getChunkCache().getWorldNames().toArray(new Key[0]),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                CACHE.getPlayerCache().isReducedDebugInfo(),
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getPlayerCache().isDoLimitedCrafting(),
                new PlayerSpawnInfo(
                    World.getCurrentDimension().id(),
                    CACHE.getChunkCache().getWorldName(),
                    CACHE.getChunkCache().getHashedSeed(),
                    GameMode.SPECTATOR,
                    GameMode.SPECTATOR,
                    CACHE.getChunkCache().isDebug(),
                    CACHE.getChunkCache().isFlat(),
                    CACHE.getPlayerCache().getLastDeathPos(),
                    CACHE.getPlayerCache().getPortalCooldown(),
                    CACHE.getChunkCache().getSeaLevel()
                ),
                false,
                false
            ));
        } else {
            EVENT_BUS.post(new PlayerConnectedEvent(connection, clientGameProfile));
            connection.send(new ClientboundLoginPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                CACHE.getChunkCache().getWorldNames().toArray(new Key[0]),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                CACHE.getPlayerCache().isReducedDebugInfo(),
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getPlayerCache().isDoLimitedCrafting(),
                new PlayerSpawnInfo(
                    World.getCurrentDimension().id(),
                    CACHE.getChunkCache().getWorldName(),
                    CACHE.getChunkCache().getHashedSeed(),
                    CACHE.getPlayerCache().getGameMode(),
                    CACHE.getPlayerCache().getGameMode(),
                    CACHE.getChunkCache().isDebug(),
                    CACHE.getChunkCache().isFlat(),
                    CACHE.getPlayerCache().getLastDeathPos(),
                    CACHE.getPlayerCache().getPortalCooldown(),
                    CACHE.getChunkCache().getSeaLevel()
                ),
                false,
                false
            ));
            if (CONFIG.debug.inventorySyncOnLogin && !Proxy.getInstance().isInQueue()) { PlayerCache.inventorySync(); }
        }
        connection.send(new ClientboundServerDataPacket(
            ZenithServerInfoBuilder.INSTANCE.getMotd(),
            Proxy.getInstance().getServerIcon()
        ));
        connection.send(new ClientboundServerLinksPacket(asList(
            new ServerLink(ServerLinkType.WEBSITE, null, "https://github.com/rfresh2/ZenithProxy"),
            new ServerLink(ServerLinkType.SUPPORT, null, "https://discord.gg/nJZrSaRKtb")
        )));
        connection.send(new ClientboundCustomReportDetailsPacket(Maps.of(
            "ZenithProxy Support", "https://discord.gg/nJZrSaRKtb"
        )));
        connection.setConfigured(true);
    }
}
