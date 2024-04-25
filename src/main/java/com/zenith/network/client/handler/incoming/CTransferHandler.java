package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.PacketHandler;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundTransferPacket;

public class CTransferHandler implements PacketHandler<ClientboundTransferPacket, ClientSession> {
    @Override
    public ClientboundTransferPacket apply(final ClientboundTransferPacket packet, final ClientSession session) {
        var reason = Component.text("Destination server requested transfer to: " + packet.getHost() + ":" + packet.getPort());
        Proxy.getInstance().getActiveConnections().forEach(c -> c.disconnect(reason));
        session.disconnect(reason);
        // todo: implement
        //  should we update the server configuration at all?
        //  or should we connect to the new server ephemerally? Still need to store some state and transfer cookie state to next client session
        //  we also need to surface what's going on to users
//        Proxy.getInstance().connectTransfer(packet.getHost(), packet.getPort());
        return null;
    }
}
