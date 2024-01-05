package com.zenith.feature.forwarding.handlers.outbound;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.zenith.module.impl.ProxyForwarding;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;

import java.util.UUID;

import static com.zenith.Shared.CACHE;

public class ForwardingPlayerInfoUpdateHandler implements PacketHandler<ClientboundPlayerInfoUpdatePacket, ServerConnection> {
    @Override
    public ClientboundPlayerInfoUpdatePacket apply(ClientboundPlayerInfoUpdatePacket packet, ServerConnection session) {
        final GameProfile clientProfile = session.getFlag(MinecraftConstants.PROFILE_KEY);
        final GameProfile cachedProfile = session.isSpectator() ? session.getSpectatorFakeProfileCache().getProfile() : CACHE.getProfileCache().getProfile();

        if (cachedProfile != null) {
            final PlayerListEntry[] entries = packet.getEntries().clone();

            for (int i = 0; i < entries.length; i++) {
                final PlayerListEntry entry = entries[i];

                UUID newId = entry.getProfileId();

                if (entry.getProfileId().equals(cachedProfile.getId())) {
                    newId = clientProfile.getId();
                } else if (entry.getProfileId().equals(clientProfile.getId())) {
                    newId = ProxyForwarding.getFakeUuid(entry.getProfileId());
                }

                if (!newId.equals(entry.getProfileId())) {
                    final GameProfile newProfile = new GameProfile(newId, entry.getName());
                    newProfile.setProperties(entry.getProfile().getProperties());

                    entries[i] = new PlayerListEntry(newId, newProfile, entry.isListed(),
                            entry.getLatency(), entry.getGameMode(), entry.getDisplayName(), entry.getSessionId(),
                            entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature());
                }
            }

            return packet.withEntries(entries);
        }

        return packet;
    }
}
