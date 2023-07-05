package com.zenith.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.module.Module;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class AutoReply extends Module {
    private final Duration replyRateLimitDuration = Duration.ofSeconds(1);
    private Cache<String, String> repliedPlayersCache;
    private Instant lastReply;

    public AutoReply() {
        super();
        this.repliedPlayersCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CONFIG.client.extra.autoReply.cooldownSeconds, TimeUnit.SECONDS)
                .build();
        this.lastReply = Instant.now();
    }

    public void updateCooldown(final int newCooldown) {
        CONFIG.client.extra.autoReply.cooldownSeconds = newCooldown;
        Cache<String, String> newCache = CacheBuilder.newBuilder()
                .expireAfterWrite(newCooldown, TimeUnit.SECONDS)
                .build();
        newCache.putAll(this.repliedPlayersCache.asMap());
        this.repliedPlayersCache = newCache;
    }

    @Subscribe
    public void handleServerChatReceivedEvent(ServerChatReceivedEvent event) {
        if (CONFIG.client.extra.autoReply.enabled && isNull(Proxy.getInstance().getCurrentPlayer().get())) {
            try {
                if (event.isWhisper && event.sender.isPresent()) {
                    if (!event.sender.get().getName().equalsIgnoreCase(CONFIG.authentication.username)
                            && Instant.now().minus(replyRateLimitDuration).isAfter(lastReply)
                            && (DISCORD_BOT.lastRelaymessage.isEmpty()
                            || Instant.now().minus(Duration.ofSeconds(CONFIG.client.extra.autoReply.cooldownSeconds)).isAfter(DISCORD_BOT.lastRelaymessage.get()))) {
                        if (isNull(repliedPlayersCache.getIfPresent(event.sender.get().getName()))) {
                            repliedPlayersCache.put(event.sender.get().getName(), event.sender.get().getName());
                            // 236 char max ( 256 - 4(command) - 16(max name length) )
                            Proxy.getInstance().getClient().send(new ClientChatPacket("/w " + event.sender.get().getName() + " " + CONFIG.client.extra.autoReply.message.substring(0, 236)));
                            this.lastReply = Instant.now();
                        }
                    }
                }
            } catch (final Throwable e) {
                CLIENT_LOG.error("AutoReply Failed", e);
            }
        }
    }
}
