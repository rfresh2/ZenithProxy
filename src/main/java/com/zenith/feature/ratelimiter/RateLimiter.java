package com.zenith.feature.ratelimiter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.network.server.ServerConnection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.SERVER_LOG;

public class RateLimiter {
    // todo: support updating rate limit seconds at runtime?
    private final Cache<InetAddress, Long> cache;

    public RateLimiter(final int rateLimitSeconds) {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(rateLimitSeconds, TimeUnit.SECONDS)
            .build();
    }

    public boolean isRateLimited(final ServerConnection session) {
        try {
            var address = ((InetSocketAddress) session.getRemoteAddress()).getAddress();
            var time = System.currentTimeMillis();
            long v = cache.get(address, () -> time);
            return time != v;
        } catch (final Throwable e) {
            SERVER_LOG.warn("Error checking rate limit for session: {}", session.getRemoteAddress(), e);
            return false;
        }
    }
}
