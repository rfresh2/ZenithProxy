package com.zenith.feature.ratelimiter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.network.server.ServerSession;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.SERVER_LOG;

public class LoginRateLimiter {
    // todo: support updating rate limit seconds at runtime?
    private final Cache<InetAddress, Long> cache;

    public LoginRateLimiter() {
        this(CONFIG.server.loginRateLimiter.rateLimitSeconds);
    }

    public LoginRateLimiter(final int rateLimitSeconds) {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(rateLimitSeconds, TimeUnit.SECONDS)
            .build();
    }

    public boolean isRateLimited(final ServerSession session) {
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

    public void reset(final ServerSession session) {
        var address = ((InetSocketAddress) session.getRemoteAddress()).getAddress();
        cache.invalidate(address);
    }
}
