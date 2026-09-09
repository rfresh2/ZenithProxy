package com.zenith.plugin.bootstrap;

import com.zenith.discord.Embed;

import static com.zenith.Globals.DISCORD;
import static com.zenith.Globals.PLUGIN_LOG;
import static com.zenith.discord.DiscordBot.escape;

/**
 * Application-side consumer
 */
public final class BootstrapMessageConsumer {
    private BootstrapMessageConsumer() {}

    public static synchronized void drainLogs() {
        for (var message = BootstrapMessages.pollLog(); message != null; message = BootstrapMessages.pollLog()) {
            if (message.failure() == null) PLUGIN_LOG.warn(message.text());
            else PLUGIN_LOG.error(message.text(), message.failure());
        }
    }

    public static synchronized void drain() {
        drainLogs();
        for (var message = BootstrapMessages.pollNotification(); message != null; message = BootstrapMessages.pollNotification()) {
            var description = escape(message.text());
            if (description.length() > 4000) description = description.substring(0, 4000);
            DISCORD.sendEmbedMessage(Embed.builder()
                .title("Plugin Bootstrap Error")
                .description(description)
                .errorColor());
        }
    }
}
