package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.zenith.Shared.CACHE;

public class ForwardingPlayerInfoRemoveHandler implements PacketHandler<ClientboundPlayerInfoRemovePacket, ServerConnection>  {
    @Override
    public ClientboundPlayerInfoRemovePacket apply(ClientboundPlayerInfoRemovePacket packet, ServerConnection session) {
        if (!session.isSpectator()) {
            final GameProfile cachedProfile = CACHE.getProfileCache().getProfile();

            if (cachedProfile != null) {
                final List<UUID> profileIds = new ArrayList<>(packet.getProfileIds());

                profileIds.replaceAll(uuid -> {
                    if (uuid.equals(cachedProfile.getId())) {
                        final GameProfile clientProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);

                        return clientProfile.getId();
                    }
                    return uuid;
                });

                return packet.withProfileIds(profileIds);
            }
        }

        return packet;
    }
}
