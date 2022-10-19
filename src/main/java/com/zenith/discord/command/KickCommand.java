package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.server.ServerConnection;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;

public class KickCommand extends Command {
    public KickCommand(Proxy proxy) {
        super(proxy, "kick", "Kick a user from the proxy. Only usable by account owners"
                + "\nUsage:"
                + "\n " + CONFIG.discord.prefix + "kick <player>"
        );
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        this.validateUserHasAccountOwnerRole(event);
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() == 2) {
            final String playerName = commandArgs.get(1);
            List<ServerConnection> connections = this.proxy.getServerConnections().stream()
                    .filter(connection -> connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName))
                    .collect(Collectors.toList());
            if (!connections.isEmpty()) {
                connections.forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                embedBuilder
                        .title("Kicked " + escape(playerName))
                        .color(Color.CYAN);
            } else {
                embedBuilder
                        .title("Unable to kick " + escape(playerName))
                        .color(Color.RUBY)
                        .addField("Reason", "Player is not connected", false);
            }
        } else {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        }

        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder.build())
                .build().asRequest();
    }
}
