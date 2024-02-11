package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.Proxy;
import com.zenith.event.module.OutboundChatEvent;
import com.zenith.module.Module;
import com.zenith.util.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;

public class AntiLeak extends Module {
    private final Pattern notNumber = Pattern.compile("[^0-9]");

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, OutboundChatEvent.class, this::handleOutgoingChat);
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.antiLeak.enabled;
    }

    public void handleOutgoingChat(final OutboundChatEvent event) {
        final String message = event.getPacket().getMessage();
        final String numbersStr = notNumber.matcher(message).replaceAll(" ");
        final String[] split = numbersStr.split(" ");
        if (split.length == 0) return;
        final List<Double> numbers = new ArrayList<>();
        for (String word : split) {
            try {
                numbers.add(Double.valueOf(word));
            } catch (final Exception e) {
                // fall through
            }
        }
        // if there less than 2 numbers, nothing to cancel
        if(numbers.size() < 2) return;
        if (CONFIG.client.extra.antiLeak.rangeCheck) {
            for (final double number : numbers) {
                var n = Math.abs(number);
                var playerX = Math.abs(CACHE.getPlayerCache().getX());
                var playerZ = Math.abs(CACHE.getPlayerCache().getZ());
                var rangeFactor = CONFIG.client.extra.antiLeak.rangeFactor;
                // essentially we're checking if any number is within a window of the player's position
                // e.g. (500, 800) with a rangeFactor of 10 will cancel if a number is 50-5000 or 80-8000
                if ((n > playerX / rangeFactor && n < playerX * rangeFactor)
                    || (n > playerZ / rangeFactor && n < playerZ * rangeFactor)) {
                    event.cancel();
                    break;
                }
            }
        } else
            event.cancel();
        if (event.isCancelled()) {
            MODULE_LOG.info("AntiLeak cancelled chat message: " + message);
            if (Proxy.getInstance().hasActivePlayer())
                Proxy.getInstance().getCurrentPlayer().get()
                    .sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&ZenithProxy&7]&r &cAntiLeak Cancelled Chat"), false));
        }
    }
}
