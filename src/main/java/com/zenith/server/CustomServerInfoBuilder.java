package com.zenith.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.packetlib.Session;
import com.zenith.Proxy;
import com.zenith.util.Queue;

import java.time.Instant;

import static com.zenith.util.Constants.CONFIG;

public class CustomServerInfoBuilder implements ServerInfoBuilder {
    private Proxy proxy;

    public CustomServerInfoBuilder(final Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public ServerStatusInfo buildInfo(Session session) {
        return new ServerStatusInfo(
                new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                new PlayerInfo(
                        CONFIG.server.ping.maxPlayers,
                        this.proxy.getCurrentPlayer().get() == null ? 0 : 1,
                        new GameProfile[0]
                ),
                getMotd(),
                this.proxy.getServerIcon(),
                true
        );
    }

    private String getMotd() {
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
                                + (this.proxy.getIsPrio().get() ? Queue.getQueueStatus().prio : Queue.getQueueStatus().regular)
                            : "Queueing")
                    + "§r§f]§r"
                    : "§cQueuing§r");
        } else {
            return "§aIn Game§r";
        }
    }

    public String getOnlineTime() {
        long milliOnline = Instant.now().toEpochMilli() - this.proxy.getConnectTime().toEpochMilli();
        // hours:minutes:seconds
        return (milliOnline / 3600000) + " : " + ((milliOnline / 60000) % 60) + " : " + ((milliOnline / 1000) % 60);
    }

}
