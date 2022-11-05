package com.zenith.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.RefStrings;

public class ServerPluginMessagePacketHandler implements HandlerRegistry.OutgoingHandler<ServerPluginMessagePacket, ServerConnection> {


    @Override
    public ServerPluginMessagePacket apply(ServerPluginMessagePacket packet, ServerConnection session) {
        if (packet.getChannel().equals("MC|Brand")) {
            return new ServerPluginMessagePacket(packet.getChannel(), RefStrings.BRAND_ENCODED);
        } else {
            return packet;
        }
    }

    @Override
    public Class<ServerPluginMessagePacket> getPacketClass() {
        return ServerPluginMessagePacket.class;
    }

    // ignore this, skidded ServerTablistDataOutgoingHandler
//    public String insertProxyBrand(final String beforeData) {
//        try {
//            MCTextRoot mcTextRoot = JsonTextParser.DEFAULT.parse(beforeFooter);
//            mcTextRoot.pushChild(new TextComponentString("§b§l" + CACHE.getProfileCache().getProfile().getName() + " §r§7[§r§3" + session.getPing() + "ms§r§r§7]§r§a -> §r§b§l" + session.getProfileCache().getProfile().getName() + " §r§7[§r§3" + Proxy.getInstance().getClient().getPing() + "ms§r§r§7]§r\n"));
//            mcTextRoot.pushChild(new TextComponentString("§9Online: §r§b§l" + getOnlineTime() + " §r§a-§r §r§9TPS: §r§b§l" + session.getProxy().getTpsCalculator().getTPS() + "§r\n"));
//            return ServerChatPacket.escapeText(mcTextRoot.toRawString());
//        } catch (final Exception e) {
//            SERVER_LOG.warn("Failed injecting proxy brand", e);
//            return beforeData;
//        }
//    }
}
