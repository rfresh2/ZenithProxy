package com.zenith.cache.data.cookie;

import com.zenith.cache.CachedData;
import lombok.Data;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCookieRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundStoreCookiePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;
import static java.util.Arrays.asList;

// todo: generify this to support both ClientSession and ServerConnection caches?
@Data
public class CookieCache implements CachedData {
    private static final String zenithTransferSrcKey = "zenith-transfer-src";
    private static final String zenithSpectatorKey = "zenith-spectator";
    private static final List<String> zenithCookies = asList(zenithTransferSrcKey, zenithSpectatorKey);
    private final Map<String, @Nullable String> cookies = new HashMap<>(2);

    public Optional<Boolean> getSpectatorCookieValue() {
        var value = cookies.get(zenithSpectatorKey);
        if (value == null) return Optional.empty();
        return Optional.of(Boolean.parseBoolean(value));
    }

    public boolean receivedAllCookieResponses() {
        return zenithCookies.stream().allMatch(cookies::containsKey);
    }

    /**
     * S2C packets requesting cookie responses
     */
    @Override
    public void getPackets(@NotNull final Consumer<Packet> consumer) {
        zenithCookies.forEach(c -> consumer.accept(new ClientboundCookieRequestPacket(c)));
    }

    public void getStoreSrcPacket(@NotNull final Consumer<Packet> consumer) {
        consumer.accept(new ClientboundStoreCookiePacket(zenithTransferSrcKey, CONFIG.server.proxyIP.getBytes()));
    }

    public void getStoreSpectatorDestPacket(@NotNull final Consumer<Packet> consumer, final boolean spectator) {
        consumer.accept(new ClientboundStoreCookiePacket(zenithSpectatorKey, String.valueOf(spectator).getBytes()));
    }

    @Override
    public void reset(final boolean full) {
        if (full) {
            cookies.clear();
        }
    }

    public void handleCookieResponse(final String key, byte @Nullable [] value) {
        if (value == null) {
            cookies.put(key, null);
        } else {
            try {
                var val = new String(value);
                cookies.put(key, val);
                if (!zenithCookies.contains(key))
                    SERVER_LOG.debug("Received unrequested cookie response: {} : {}", key, val);
            } catch (final Throwable e) {
                SERVER_LOG.debug("Unable to parse cookie response to string for key: {}", key, e);
            }
        }
    }
}
