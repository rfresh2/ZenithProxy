package com.zenith.cache.data;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.cache.CachedData;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ServerProfileCache implements CachedData {

    protected GameProfile profile;

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {}

    public @Nullable GameProfile getProfile() {
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
