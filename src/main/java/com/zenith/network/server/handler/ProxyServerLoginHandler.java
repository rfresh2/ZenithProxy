package com.zenith.network.server.handler;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;
import com.github.steveice10.packetlib.Session;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.event.proxy.PlayerLoginEvent;
import com.zenith.event.proxy.ProxyClientConnectedEvent;
import com.zenith.event.proxy.ProxySpectatorConnectedEvent;
import com.zenith.network.server.CustomServerInfoBuilder;
import com.zenith.network.server.ProxyServerListener;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.Wait;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class ProxyServerLoginHandler implements ServerLoginHandler {
    private final Proxy proxy;

    public ProxyServerLoginHandler(final Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public void loggedIn(Session session) {
        final GameProfile clientGameProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        ServerConnection connection = ((ProxyServerListener) this.proxy.getServer().getListeners().stream()
                .filter(ProxyServerListener.class::isInstance)
                .findAny().orElseThrow(IllegalStateException::new))
                .getConnections().get(session);
        SERVER_LOG.info("Player connected: UUID: {}, Username: {}, Address: {}", clientGameProfile.getId(), clientGameProfile.getName(), connection.getRemoteAddress());

        if (!Wait.waitUntilCondition(() -> Proxy.getInstance().isConnected()
                        && (this.proxy.getConnectTime().isBefore(Instant.now().minus(Duration.of(3, ChronoUnit.SECONDS))) || Proxy.getInstance().isInQueue())
                        && CACHE.getPlayerCache().getEntityId() != -1
                        && nonNull(CACHE.getProfileCache().getProfile())
                        && nonNull(CACHE.getPlayerCache().getGameMode())
                        && nonNull(CACHE.getChunkCache().getCurrentDimension())
                        && nonNull(CACHE.getChunkCache().getWorldData())
                        && nonNull(CACHE.getTabListCache().get(CACHE.getProfileCache().getProfile().getId()))
                        && connection.isWhitelistChecked(),
                20)) {
            session.disconnect("Client login timed out.");
            return;
        }
        connection.setPlayer(true);
        EVENT_BUS.post(new PlayerLoginEvent(connection));
        if (connection.isSpectator()) {
            if (Proxy.getInstance().getCurrentPlayer().get() == null) {
                // can probably make this state work with some more work but im just gonna block it for now
                connection.disconnect("A player must be connected in order to spectate!");
                return;
            }
            EVENT_BUS.post(new ProxySpectatorConnectedEvent(clientGameProfile));
            session.send(new ClientboundLoginPacket(
                connection.getSpectatorSelfEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                GameMode.SPECTATOR,
                GameMode.SPECTATOR,
                CACHE.getChunkCache().getDimensionRegistry().keySet().toArray(String[]::new),
                CACHE.getChunkCache().getRegistryTag(),
                CACHE.getChunkCache().getWorldData().dimensionType(),
                CACHE.getChunkCache().getWorldData().worldName(),
                CACHE.getChunkCache().getWorldData().hashedSeed(),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                false,
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getChunkCache().getWorldData().debug(),
                CACHE.getChunkCache().getWorldData().flat(),
                CACHE.getPlayerCache().getLastDeathPos(),
                CACHE.getPlayerCache().getPortalCooldown()
            ));
        } else {
            EVENT_BUS.post(new ProxyClientConnectedEvent(clientGameProfile));
            session.send(new ClientboundLoginPacket(
                CACHE.getPlayerCache().getEntityId(),
                CACHE.getPlayerCache().isHardcore(),
                CACHE.getPlayerCache().getGameMode(),
                CACHE.getPlayerCache().getGameMode(),
                CACHE.getChunkCache().getDimensionRegistry().keySet().toArray(String[]::new),
                CACHE.getChunkCache().getRegistryTag(),
                CACHE.getChunkCache().getWorldData().dimensionType(),
                CACHE.getChunkCache().getWorldData().worldName(),
                CACHE.getChunkCache().getWorldData().hashedSeed(),
                CACHE.getPlayerCache().getMaxPlayers(),
                CACHE.getChunkCache().getServerViewDistance(),
                CACHE.getChunkCache().getServerSimulationDistance(),
                false,
                CACHE.getPlayerCache().isEnableRespawnScreen(),
                CACHE.getChunkCache().getWorldData().debug(),
                CACHE.getChunkCache().getWorldData().flat(),
                CACHE.getPlayerCache().getLastDeathPos(),
                CACHE.getPlayerCache().getPortalCooldown()
            ));
            if (!proxy.isInQueue()) { PlayerCache.sync(); }
            CustomServerInfoBuilder serverInfoBuilder = Proxy.getInstance().getServer().getGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
            session.send(new ClientboundServerDataPacket(
                Component.text(serverInfoBuilder.getMotd()),
                Proxy.getInstance().getServerIcon(),
                false
            ));
        }
    }
}
