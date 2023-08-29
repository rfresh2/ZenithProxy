package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.zenith.Proxy;
import com.zenith.feature.queue.Queue;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;
import net.daporkchop.lib.logging.format.component.TextComponentString;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.JsonTextParser;

import java.time.Instant;

import static com.zenith.Shared.*;

public class ServerTablistDataOutgoingHandler implements OutgoingHandler<ClientboundTabListPacket, ServerConnection> {

    @Override
    public ClientboundTabListPacket apply(ClientboundTabListPacket packet, ServerConnection session) {
        return packet;
//        return new ClientboundTabListPacket(packet.getHeaderRaw(), insertProxyDataIntoFooter(packet.getFooterRaw(), session));
    }

    @Override
    public Class<ClientboundTabListPacket> getPacketClass() {
        return ClientboundTabListPacket.class;
    }

    public String insertProxyDataIntoFooter(final String beforeFooter, final ServerConnection session) {
        try {
            MCTextRoot mcTextRoot = JsonTextParser.DEFAULT.parse(beforeFooter);
            // todo: legacy formatting codes might not be supported
            mcTextRoot.pushChild(new TextComponentString("\n§b§lZenithProxy§r\n"));
            mcTextRoot.pushChild(new TextComponentString("§b§l " + session.getProfileCache().getProfile().getName() + " §r§7[§r§3" + session.getPing() + "ms§r§r§7]§r§a -> §r§b§l" + CACHE.getProfileCache().getProfile().getName() + " §r§7[§r§3" + Proxy.getInstance().getClient().getPing() + "ms§r§r§7]§r\n"));
            mcTextRoot.pushChild(new TextComponentString("§9Online: §r§b§l" + getOnlineTime() + " §r§a-§r §r§9TPS: §r§b§l" + TPS_CALCULATOR.getTPS() + "§r\n"));
            // todo: may need to escape the text
            return mcTextRoot.toRawString();
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
