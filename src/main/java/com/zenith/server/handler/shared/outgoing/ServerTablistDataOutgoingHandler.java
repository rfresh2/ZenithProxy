package com.zenith.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.zenith.Proxy;
import com.zenith.server.ServerConnection;
import com.zenith.util.Queue;
import com.zenith.util.handler.HandlerRegistry;
import net.daporkchop.lib.logging.format.component.TextComponentString;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.JsonTextParser;

import java.time.Instant;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.SERVER_LOG;

public class ServerTablistDataOutgoingHandler implements HandlerRegistry.OutgoingHandler<ServerPlayerListDataPacket, ServerConnection> {


    @Override
    public ServerPlayerListDataPacket apply(ServerPlayerListDataPacket packet, ServerConnection session) {
        return new ServerPlayerListDataPacket(packet.getHeader(), insertProxyDataIntoFooter(packet.getFooter(), session), false);
    }

    @Override
    public Class<ServerPlayerListDataPacket> getPacketClass() {
        return ServerPlayerListDataPacket.class;
    }

    public String insertProxyDataIntoFooter(final String beforeFooter, final ServerConnection session) {
        try {
            MCTextRoot mcTextRoot = JsonTextParser.DEFAULT.parse(beforeFooter);
            mcTextRoot.pushChild(new TextComponentString("§b§l" + CACHE.getProfileCache().getProfile().getName() + " §r§7[§r§3" + session.getPing() + "ms§r§r§7]§r§a -> §r§b§l" + session.getProfileCache().getProfile().getName() + " §r§7[§r§3" + Proxy.getInstance().getClient().getPing() + "ms§r§r§7]§r\n"));
            mcTextRoot.pushChild(new TextComponentString("§9Online: §r§b§l" + getOnlineTime() + " §r§a-§r §r§9TPS: §r§b§l" + session.getProxy().getTpsCalculator().getTPS() + "§r\n"));
            return ServerChatPacket.escapeText(mcTextRoot.toRawString());
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed injecting proxy info to tablist footer", e);
            return beforeFooter;
        }
    }

    public String getOnlineTime() {
        long onlineSeconds = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
        return Queue.getEtaStringFromSeconds(onlineSeconds);
    }
}
