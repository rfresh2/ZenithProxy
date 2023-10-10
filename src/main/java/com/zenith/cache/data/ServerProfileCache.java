package com.zenith.cache.data;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundServerDataPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.network.server.CustomServerInfoBuilder;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.util.function.Consumer;

public class ServerProfileCache implements CachedData {

    protected GameProfile profile;

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        CustomServerInfoBuilder serverInfoBuilder = Proxy.getInstance().getServer().getGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY);
        consumer.accept(new ClientboundServerDataPacket(
            Component.text(serverInfoBuilder.getMotd()),
            Proxy.getInstance().getServerIcon(),
            false
        ));
    }

    public GameProfile getProfile() {
        synchronized (this) {
            return profile;
        }
    }

    public void setProfile(final GameProfile profile) {
        synchronized (this) {
            this.profile = profile;
        }
    }

    @Override
    public void reset(boolean full) {
        if (full)   {
            this.profile = null;
        }
    }
}
