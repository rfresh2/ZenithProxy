package com.zenith.feature.coordobf.handlers.outbound;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;

public class COSetDefaultSpawnPositionHandler implements PacketHandler<ClientboundSetDefaultSpawnPositionPacket, ServerSession> {
    @Override
    public ClientboundSetDefaultSpawnPositionPacket apply(final ClientboundSetDefaultSpawnPositionPacket packet, final ServerSession session) {
        // no need for clients to know this
        // and could reveal offset under certain conditions
        return new ClientboundSetDefaultSpawnPositionPacket(new GlobalPos(Key.key("overworld"), 0, 0, 0), packet.getYaw(), packet.getPitch());
    }
}
