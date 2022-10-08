package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.proxy.ServerChatReceivedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.CLIENT_LOG;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Objects.isNull;

public class AutoReply extends Module {
    private final Cache<String, String> repliedPlayersCache;
    private final Duration replyRateLimitDuration = Duration.ofSeconds(3);
    private Instant lastReply;

    public AutoReply(Proxy proxy) {
        super(proxy);
        this.repliedPlayersCache = CacheBuilder.newBuilder()
                .expireAfterWrite(20L, TimeUnit.MINUTES)
                .build();
        this.lastReply = Instant.now();
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (CONFIG.client.extra.autoReply.enabled && isNull(this.proxy.getCurrentPlayer().get())) {
            try {
                if (!event.message.startsWith("<")) {
                    String[] split = event.message.split(" ");
                    if (split.length > 2) {
                        final String sender = split[0];
                        if (split[1].startsWith("whispers")
                                && !sender.equalsIgnoreCase(CONFIG.authentication.username)
                                && Instant.now().minus(replyRateLimitDuration).isAfter(lastReply)) {
                            if (isNull(repliedPlayersCache.getIfPresent(sender))) {
                                repliedPlayersCache.put(sender, sender);
                                // 236 char max ( 256 - 4(command) - 16(max name length)
                                this.proxy.getClient().send(new ClientChatPacket("/w " + sender + " " + CONFIG.client.extra.autoReply.message));
                                this.lastReply = Instant.now();
                            }
                        }
                    }
                }
            } catch (final Throwable e) {
                CLIENT_LOG.error("AutoReply Failed", e);
            }
        }
    }
}
