package com.zenith.discord.command;

import com.zenith.Proxy;
import com.zenith.util.spectator.SpectatorEntityRegistry;
import com.zenith.util.spectator.entity.SpectatorEntity;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.saveConfig;

public class SpectatorCommand extends Command {
    public SpectatorCommand(Proxy proxy) {
        super(proxy, "spectator", "Enable or disable the Spectator feature for whitelisted users."
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "spectator on/off"
                + "\n  " + CONFIG.discord.prefix + "spectator whitelist add/del <player>"
                + "\n  " + CONFIG.discord.prefix + "spectator whitelist list"
                + "\n  " + CONFIG.discord.prefix + "spectator entity list"
                + "\n  " + CONFIG.discord.prefix + "spectator entity <entity>"
                + "\n  " + CONFIG.discord.prefix + "spectator chat on/off");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();
        validateUserHasAccountOwnerRole(event);

        if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            CONFIG.server.spectator.allowSpectator = true;
            embedBuilder
                    .title("Spectators On!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.server.spectator.allowSpectator = false;
            this.proxy.getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
            embedBuilder
                    .title("Spectators Off!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("entity")) {
            if (commandArgs.size() == 3) {
                if (commandArgs.get(2).equalsIgnoreCase("list")) {
                    embedBuilder
                            .title("Entity List")
                            .addField("Entity", String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()), false)
                            .color(Color.CYAN);
                } else {
                    Optional<SpectatorEntity> spectatorEntity = SpectatorEntityRegistry.getSpectatorEntity(commandArgs.get(2));
                    if (spectatorEntity.isPresent()) {
                        CONFIG.server.spectator.spectatorEntity = commandArgs.get(2);
                        embedBuilder
                                .title("Set Entity")
                                .addField("Entity", commandArgs.get(2), false)
                                .color(Color.CYAN);
                    } else {
                        embedBuilder
                                .title("Invalid Entity")
                                .addField("Valid Entities", String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()), false)
                                .color(Color.RUBY);
                    }
                }
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("whitelist")) {
            if (commandArgs.size() == 4) {
                if (commandArgs.get(2).equalsIgnoreCase("add")) {
                    final String playerName = commandArgs.get(3);
                    if (!CONFIG.server.spectator.spectatorWhitelist.contains(playerName)) {
                        CONFIG.server.spectator.spectatorWhitelist.add(playerName);
                    }
                    embedBuilder
                            .title("Added user: " + escape(playerName) + " To Spectator Whitelist")
                            .color(Color.CYAN)
                            .addField("Spectator Whitelist",
                                    escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                            ? "Empty"
                                            : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
                } else if (commandArgs.get(2).equalsIgnoreCase("del")) {
                    final String playerName = commandArgs.get(3);
                    CONFIG.server.spectator.spectatorWhitelist.removeIf(s -> s.equalsIgnoreCase(playerName));
                    embedBuilder
                            .title("Removed user: " + escape(playerName) + " From Spectator Whitelist")
                            .color(Color.CYAN)
                            .addField("Spectator Whitelist",
                                    escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                            ? "Empty"
                                            : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
                } else {
                    embedBuilder
                            .title("Invalid command usage")
                            .addField("Usage", this.description, false)
                            .color(Color.RUBY);
                }
            } else if (commandArgs.get(2).equalsIgnoreCase("list")) {
                embedBuilder
                        .title("Spectator Whitelist")
                        .color(Color.CYAN)
                        .addField("Spectator Whitelist",
                                escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                        ? "Empty"
                                        : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else if (commandArgs.get(1).equalsIgnoreCase("chat")) {
            if (commandArgs.size() == 3) {
                if (commandArgs.get(2).equalsIgnoreCase("on")) {
                    CONFIG.server.spectator.spectatorPublicChatEnabled = true;
                    embedBuilder
                            .title("Spectator Chat Enabled")
                            .color(Color.CYAN);
                } else if (commandArgs.get(2).equalsIgnoreCase("off")) {
                    CONFIG.server.spectator.spectatorPublicChatEnabled = false;
                    embedBuilder
                            .title("Spectator Chat Disabled")
                            .color(Color.CYAN);
                } else {
                    embedBuilder
                            .title("Invalid command usage")
                            .addField("Usage", this.description, false)
                            .color(Color.RUBY);
                }
            } else {
                embedBuilder
                        .title("Invalid command usage")
                        .addField("Usage", this.description, false)
                        .color(Color.RUBY);
            }
        } else {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        }

        saveConfig();
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build().asRequest();
    }
}
