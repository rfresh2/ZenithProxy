package com.zenith.discord.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zenith.Proxy;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import com.zenith.util.spectator.SpectatorEntityRegistry;
import com.zenith.util.spectator.entity.SpectatorEntity;
import discord4j.rest.util.Color;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.Constants.CONFIG;
import static java.util.Arrays.asList;

public class SpectatorCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "spectator",
                "Configure the Spectator feature",
                asList("on/off",
                        "whitelist add/del <player>", "whitelist list",
                        "entity list", "entity <entity>",
                        "chat on/off")
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("spectator").requires(this::validateAccountOwner)
                        .then(literal("on").executes(c -> {
                            CONFIG.server.spectator.allowSpectator = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Spectators On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.server.spectator.allowSpectator = false;
                            Proxy.getInstance().getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                            c.getSource().getEmbedBuilder()
                                    .title("Spectators Off!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("whitelist")
                                .then(literal("add").then(argument("player", string()).executes(c -> {
                                    final String playerName = StringArgumentType.getString(c, "player");
                                    if (!CONFIG.server.spectator.spectatorWhitelist.contains(playerName)) {
                                        CONFIG.server.spectator.spectatorWhitelist.add(playerName);
                                    }
                                    c.getSource().getEmbedBuilder()
                                            .title("Added user: " + escape(playerName) + " To Spectator Whitelist")
                                            .color(Color.CYAN)
                                            .addField("Spectator Whitelist",
                                                    escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                                            ? "Empty"
                                                            : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
                                    return 1;
                                })))
                                .then(literal("del").then(argument("player", string()).executes(c -> {
                                    final String playerName = StringArgumentType.getString(c, "player");
                                    CONFIG.server.spectator.spectatorWhitelist.removeIf(s -> s.equalsIgnoreCase(playerName));
                                    c.getSource().getEmbedBuilder()
                                            .title("Removed user: " + escape(playerName) + " From Spectator Whitelist")
                                            .color(Color.CYAN)
                                            .addField("Spectator Whitelist",
                                                    escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                                            ? "Empty"
                                                            : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
                                    return 1;
                                })))
                                .then(literal("list").executes(c -> {
                                    c.getSource().getEmbedBuilder()
                                            .title("Spectator Whitelist")
                                            .color(Color.CYAN)
                                            .addField("Spectator Whitelist",
                                                    escape(CONFIG.server.spectator.spectatorWhitelist.isEmpty()
                                                            ? "Empty"
                                                            : String.join(", ", CONFIG.server.spectator.spectatorWhitelist)), false);
                                })))
                        .then(literal("entity")
                                .then(literal("list").executes(c -> {
                                    c.getSource().getEmbedBuilder()
                                            .title("Entity List")
                                            .addField("Entity", String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()), false)
                                            .color(Color.CYAN);
                                }))
                                .then(argument("entityID", string()).executes(c -> {
                                    final String entityInput = StringArgumentType.getString(c, "entityID");
                                    Optional<SpectatorEntity> spectatorEntity = SpectatorEntityRegistry.getSpectatorEntity(entityInput);
                                    if (spectatorEntity.isPresent()) {
                                        CONFIG.server.spectator.spectatorEntity = entityInput;
                                        c.getSource().getEmbedBuilder()
                                                .title("Set Entity")
                                                .addField("Entity", entityInput, false)
                                                .color(Color.CYAN);
                                    } else {
                                        c.getSource().getEmbedBuilder()
                                                .title("Invalid Entity")
                                                .addField("Valid Entities", String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()), false)
                                                .color(Color.RUBY);
                                    }
                                    return 1;
                                })))
                        .then(literal("chat")
                                .then(literal("on").executes(c -> {
                                    CONFIG.server.spectator.spectatorPublicChatEnabled = true;
                                    c.getSource().getEmbedBuilder()
                                            .title("Spectator Chat Enabled")
                                            .color(Color.CYAN);
                                }))
                                .then(literal("off").executes(c -> {
                                    CONFIG.server.spectator.spectatorPublicChatEnabled = false;
                                    c.getSource().getEmbedBuilder()
                                            .title("Spectator Chat Disabled")
                                            .color(Color.CYAN);
                                })))
        );
    }
}
