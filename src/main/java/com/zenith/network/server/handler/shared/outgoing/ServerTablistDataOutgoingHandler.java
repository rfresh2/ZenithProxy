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
            return footer.append(Component.text().appendNewline().append(ComponentSerializer.minedown("&b&lZenithProxy&r")).build())
                .append(Component.text()
                            .appendNewline()
                            .append(ComponentSerializer.minedown(
                             "&b&l " + sessionProfileName
                                 + " &r&7[&r&3" + session.getPing() + "ms&r&7]&r&7"
                                 + " -> &r&b&l" + clientProfileName
                                 + " &r&7[&r&3" + Proxy.getInstance().getClient().getPing() + "ms&r&7]&r"
                            )).build())
                .append(Component.text()
                         .appendNewline()
                         .append(ComponentSerializer.minedown(
                             "&9Online: &r&b&l" + Proxy.getInstance().getOnlineTimeString() + " &r&7-&r &r&9TPS: &r&b&l" +
                                 TPS.getTPS() + "&r")).build());
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed injecting proxy info to tablist footer", e);
            return footer;
        }
    }
}
