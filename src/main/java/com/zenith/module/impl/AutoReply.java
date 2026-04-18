package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zenith.Proxy;
import com.zenith.event.chat.WhisperChatEvent;
import com.zenith.module.api.Module;
import com.zenith.util.ChatUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.DISCORD;
import static java.util.Objects.isNull;

public class AutoReply extends Module {
    private Cache<String, String> repliedPlayersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(CONFIG.client.extra.autoReply.cooldownSeconds))
            .build();
    private Instant lastReply = Instant.now();

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(WhisperChatEvent.class, this::handleWhisperChatEvent)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoReply.enabled;
    }

    public void updateCooldown(final int newCooldown) {
        CONFIG.client.extra.autoReply.cooldownSeconds = newCooldown;
        Cache<String, String> newCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(newCooldown))
                .build();
        newCache.putAll(this.repliedPlayersCache.asMap());
        this.repliedPlayersCache = newCache;
    }

    private void handleWhisperChatEvent(WhisperChatEvent event) {
        if (Proxy.getInstance().hasActivePlayer()) return;
        if (event.outgoing()) return;
        if (!event.sender().getName().equalsIgnoreCase(CONFIG.authentication.username)
            && Instant.now().minus(Duration.ofSeconds(1)).isAfter(lastReply)
            && (DISCORD.lastRelayMessage.isEmpty()
            || Instant.now().minus(Duration.ofSeconds(CONFIG.client.extra.autoReply.cooldownSeconds)).isAfter(DISCORD.lastRelayMessage.get()))) {
            if (isNull(repliedPlayersCache.getIfPresent(event.sender().getName()))) {
                repliedPlayersCache.put(event.sender().getName(), event.sender().getName());
                sendClientPacketAsync(ChatUtil.getWhisperChatPacket(event.sender().getName(), CONFIG.client.extra.autoReply.message));
                this.lastReply = Instant.now();
            } else {
                debug("Not sending reply to {} due to cooldown", event.sender().getName());
            }
        }
    }
}
