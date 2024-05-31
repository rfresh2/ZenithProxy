package com.zenith.cache.data;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.NonNull;
import lombok.Setter;
import org.geysermc.mcprotocollib.network.packet.Packet;

import javax.annotation.Nullable;
import java.util.function.Consumer;

@Setter
public class ServerProfileCache implements CachedData {

    protected GameProfile profile;

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {}

    public @Nullable GameProfile getProfile() {
        return profile;
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL)   {
            this.profile = null;
        }
    }
}
