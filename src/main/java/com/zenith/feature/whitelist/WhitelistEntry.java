package com.zenith.feature.whitelist;

import java.util.Objects;
import java.util.UUID;

public class WhitelistEntry {
    public String username;
    public UUID uuid;
    // epoch second
    public Long lastRefreshed;

    public WhitelistEntry() {
    }

    public WhitelistEntry(final String username, final UUID uuid, final Long lastRefreshed) {
        this.username = username;
        this.uuid = uuid;
        this.lastRefreshed = lastRefreshed;
    }

    public String getNameMCLink() {
        return "https://namemc.com/profile/" + uuid.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WhitelistEntry that = (WhitelistEntry) o;
        // only check equality with username and uuid
        return Objects.equals(username, that.username) && Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, uuid);
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" +
                "username='" + username + '\'' +
                ", uuid=" + uuid +
                ", lastRefreshed=" + lastRefreshed +
                '}';
    }
}
