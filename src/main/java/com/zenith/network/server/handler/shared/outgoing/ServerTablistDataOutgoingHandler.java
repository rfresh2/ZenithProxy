package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.Proxy;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;

import static com.zenith.Shared.*;

public class ServerTablistDataOutgoingHandler implements PacketHandler<ClientboundTabListPacket, ServerSession> {

    @Override
    public ClientboundTabListPacket apply(ClientboundTabListPacket packet, ServerSession session) {
        CACHE.getTabListCache().setLastUpdate(System.currentTimeMillis());
        return new ClientboundTabListPacket(packet.getHeader(), insertProxyDataIntoFooter(packet.getFooter(), session));
    }

    public Component insertProxyDataIntoFooter(final Component footer, final ServerSession session) {
        try {
            var sessionProfile = session.getProfileCache().getProfile();
            var clientProfile = CACHE.getProfileCache().getProfile();
            var sessionProfileName = sessionProfile == null ? "Unknown" : sessionProfile.getName();
            var clientProfileName = clientProfile == null ? "Unknown" : clientProfile.getName();
            return footer.append(Component.text().appendNewline().append(ComponentSerializer.minimessage("<aqua><bold>ZenithProxy")).build())
                .append(Component.text()
                            .appendNewline()
                            .append(ComponentSerializer.minimessage(
                             "<aqua><bold> " + sessionProfileName
                                 + " </bold><gray>[<dark_aqua>" + session.getPing() + "ms<gray>]"
                                 + " <gray>-> <aqua><bold>" + clientProfileName
                                 + " </bold><gray>[<dark_aqua>" + Proxy.getInstance().getClient().getPing() + "ms<gray>]"
                            )).build())
                .append(Component.text()
                         .appendNewline()
                         .append(ComponentSerializer.minimessage(
                             "<blue>Online: <aqua><bold>" + Proxy.getInstance().getOnlineTimeString()
                                 + "</bold> <gray>- <blue>TPS: <aqua><bold>" + TPS.getTPS()))
                            .build());
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed injecting proxy info to tablist footer", e);
            return footer;
        }
    }
}
