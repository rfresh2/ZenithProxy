package com.zenith.network.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.packetlib.Session;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.feature.queue.Queue;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class CustomServerInfoBuilder implements ServerInfoBuilder {

    private final Cache<String, ServerStatusInfo> infoCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(CONFIG.server.ping.responseCacheSeconds))
        .maximumSize(10)
        .build();

    @Override
    public ServerStatusInfo buildInfo(Session session) {
        if (!CONFIG.server.ping.enabled) return null;
        if (CONFIG.server.ping.responseCaching) {
            var cacheKey = getSessionCacheKey(session);
            try {
                // building the server status here can be expensive
                // due to accessing player caches, active connections, etc
                // its possible someone could DoS a server pretty easily
                return infoCache.get(cacheKey, () -> createServerStatusInfo(session));
            } catch (ExecutionException e) {
                SERVER_LOG.debug("Failed to build server info for {}", cacheKey, e);
                return null;
            }
        } else return createServerStatusInfo(session);
    }

    private String getSessionCacheKey(Session session) {
        if (CONFIG.server.viaversion.enabled) { // our response has a different protocol version for each connection (mirroring them)
            String ip = session.getRemoteAddress().toString();
            if (ip.contains("/")) ip = ip.substring(ip.indexOf("/") + 1);
            if (ip.contains(":")) ip = ip.substring(0, ip.indexOf(":"));
            return ip;
        } else return "";
    }

    private ServerStatusInfo createServerStatusInfo(Session session) {
        return new ServerStatusInfo(
            getVersionInfo(session),
            getPlayerInfo(),
            Component.text(getMotd()),
            Proxy.getInstance().getServerIcon(),
            false
        );
    }

    private VersionInfo getVersionInfo(Session session) {
        if (CONFIG.server.viaversion.enabled && session instanceof ServerConnection)
            return new VersionInfo("ZenithProxy", ((ServerConnection) session).getProtocolVersion());
        return new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion());
    }

    private PlayerInfo getPlayerInfo() {
        if (CONFIG.server.ping.onlinePlayers) {
            return new PlayerInfo(
                CONFIG.server.ping.maxPlayers,
                Proxy.getInstance().getActiveConnections().size(),
                List.of(getOnlinePlayerProfiles())
            );
        } else {
            return new PlayerInfo(
                CONFIG.server.ping.maxPlayers,
                0,
                Collections.emptyList()
            );
        }
    }

    public GameProfile[] getOnlinePlayerProfiles() {
        try {
            return Proxy.getInstance().getActiveConnections().stream()
                    .map(connection -> connection.profileCache.getProfile())
                    .toArray(GameProfile[]::new);
        } catch (final RuntimeException e) {
            // do nothing, failsafe if we get some race condition
        }
        return new GameProfile[0];
    }

    public String getMotd() {
        String result = "§f[§r§b" + CONFIG.authentication.username + "§r§f]§r - ";
        if (Proxy.getInstance().isConnected()) {
            result += getMotdStatus();
            result += "\n§bOnline for:§r §f[§r" + getOnlineTime() + "§f]§r";
        } else {
            result += "§cDisconnected§r";
        }
        return result;
    }

    public String getMotdStatus() {
        var proxy = Proxy.getInstance();
        if (proxy.isInQueue()) {
            return (proxy.getIsPrio().isPresent()
                ? (proxy.getIsPrio().get()
                            ? "§cIn Prio Queue§r"
                            : "§cIn Queue§r")
                        + " §f[§r§b"
                          + (proxy.getQueuePosition() != Integer.MAX_VALUE
                                ? proxy.getQueuePosition() + " / "
                                    + (proxy.getIsPrio().get() ? Queue.getQueueStatus().prio() : Queue.getQueueStatus().regular())
                                : "Queueing")
                        + "§r§f]§r"
                        + ((proxy.getQueuePosition() != Integer.MAX_VALUE)
                    ? " - §cETA§r §f[§r§b" + Queue.getQueueEta(proxy.getQueuePosition()) + "§r§f]§r"
                    : "")
                    : "§cQueuing§r");
        } else {
            return "§aIn Game§r";
        }
    }

    public String getOnlineTime() {
        long onlineSeconds = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
        return Queue.getEtaStringFromSeconds(onlineSeconds);
    }

}
