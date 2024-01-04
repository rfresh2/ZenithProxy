package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import static com.zenith.Shared.CACHE;

public class ForwardingPlayerInfoUpdateHandler implements PacketHandler<ClientboundPlayerInfoUpdatePacket, ServerConnection>  {
    @Override
    public ClientboundPlayerInfoUpdatePacket apply(ClientboundPlayerInfoUpdatePacket packet, ServerConnection session) {
        if (!session.isSpectator()) {
            final GameProfile cachedProfile = CACHE.getProfileCache().getProfile();

            if (cachedProfile != null) {
                final PlayerListEntry[] entries = packet.getEntries().clone();

                for (int i = 0; i < entries.length; i++) {
                    final PlayerListEntry entry = entries[i];

                    if (entry.getProfileId().equals(cachedProfile.getId())) {
                        final GameProfile clientProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);

                        final GameProfile newProfile = new GameProfile(clientProfile.getId(), cachedProfile.getName());
                        newProfile.setProperties(cachedProfile.getProperties());

                        entries[i] = new PlayerListEntry(clientProfile.getId(), newProfile, entry.isListed(),
                                entry.getLatency(), entry.getGameMode(), entry.getDisplayName(), entry.getSessionId(),
                                entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature());
                    }
                }

                return packet.withEntries(entries);
            }
        }

        return packet;
    }
}
