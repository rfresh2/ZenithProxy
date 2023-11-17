package com.zenith.network.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.packetlib.Session;
import com.zenith.Proxy;
import com.zenith.feature.queue.Queue;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static com.zenith.Shared.CONFIG;

public class CustomServerInfoBuilder implements ServerInfoBuilder {
    private final Proxy proxy;

    public CustomServerInfoBuilder(final Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public ServerStatusInfo buildInfo(Session session) {
        if (!CONFIG.server.ping.enabled) return null;
        return new ServerStatusInfo(
            new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion()),
            getPlayerInfo(),
            Component.text(getMotd()),
            this.proxy.getServerIcon(),
            false
        );
    }

    private PlayerInfo getPlayerInfo() {
        if (CONFIG.server.ping.onlinePlayers) {
            return new PlayerInfo(
                CONFIG.server.ping.maxPlayers,
                this.proxy.getActiveConnections().size(),
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
            return this.proxy.getActiveConnections().stream()
                    .map(connection -> connection.profileCache.getProfile())
                    .toArray(GameProfile[]::new);
        } catch (final RuntimeException e) {
            // do nothing, failsafe if we get some race condition
        }
        return new GameProfile[0];
    }

    public String getMotd() {
        String result = "§f[§r§b" + CONFIG.authentication.username + "§r§f]§r - ";
        if (this.proxy.isConnected()) {
            result += getMotdStatus();
            result += "\n§bOnline for:§r §f[§r" + getOnlineTime() + "§f]§r";
        } else {
            result += "§cDisconnected§r";
        }
        return result;
    }

    public String getMotdStatus() {
        if (this.proxy.isInQueue()) {
            return (this.proxy.getIsPrio().isPresent()
                    ? (this.proxy.getIsPrio().get()
                            ? "§cIn Prio Queue§r"
                            : "§cIn Queue§r")
                        + " §f[§r§b"
                          + (this.proxy.getQueuePosition() != Integer.MAX_VALUE
                                ? this.proxy.getQueuePosition() + " / "
                                    + (this.proxy.getIsPrio().get() ? Queue.getQueueStatus().prio() : Queue.getQueueStatus().regular())
                                : "Queueing")
                        + "§r§f]§r"
                        + ((this.proxy.getQueuePosition() != Integer.MAX_VALUE)
                    ? " - §cETA§r §f[§r§b" + Queue.getQueueEta(this.proxy.getQueuePosition()) + "§r§f]§r"
                    : "")
                    : "§cQueuing§r");
        } else {
            return "§aIn Game§r";
        }
    }

    public String getOnlineTime() {
        long onlineSeconds = Instant.now().getEpochSecond() - this.proxy.getConnectTime().getEpochSecond();
        return Queue.getEtaStringFromSeconds(onlineSeconds);
    }

}
