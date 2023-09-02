package com.zenith.network.server.handler.shared.outgoing;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import com.zenith.Proxy;
import com.zenith.feature.queue.Queue;
import com.zenith.network.registry.OutgoingHandler;
import com.zenith.network.server.ServerConnection;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;

import java.time.Instant;

import static com.zenith.Shared.*;

public class ServerTablistDataOutgoingHandler implements OutgoingHandler<ClientboundTabListPacket, ServerConnection> {

    @Override
    public ClientboundTabListPacket apply(ClientboundTabListPacket packet, ServerConnection session) {
        return new ClientboundTabListPacket(packet.getHeader(), insertProxyDataIntoFooter(packet.getFooter(), session));
    }

    @Override
    public Class<ClientboundTabListPacket> getPacketClass() {
        return ClientboundTabListPacket.class;
    }

    public Component insertProxyDataIntoFooter(final Component footer, final ServerConnection session) {
        try {
            return footer.append(Component.text().appendNewline().append(MineDown.parse("&b&lZenithProxy&r")).build())
                .append(Component.text()
                            .appendNewline()
                            .append(MineDown.parse(
                             "&b&l " + session.getProfileCache().getProfile().getName() + " &r&7[&r&3" +
                                 session.getPing() + "ms&r&7]&r&7 -> &r&b&l" +
                                 CACHE.getProfileCache().getProfile().getName() + " &r&7[&r&3" +
                                 Proxy.getInstance().getClient().getPing() + "ms&r&7]&r")).build())
                .append(Component.text()
                         .appendNewline()
                         .append(MineDown.parse(
                             "&9Online: &r&b&l" + getOnlineTime() + " &r&7-&r &r&9TPS: &r&b&l" +
                                 TPS_CALCULATOR.getTPS() + "&r")).build());
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed injecting proxy info to tablist footer", e);
            return footer;
        }
    }

    public String getOnlineTime() {
        long onlineSeconds = Instant.now().getEpochSecond() - Proxy.getInstance().getConnectTime().getEpochSecond();
        return Queue.getEtaStringFromSeconds(onlineSeconds);
    }
}
