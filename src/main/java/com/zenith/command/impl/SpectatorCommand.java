package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.entity.SpectatorEntity;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.WHITELIST_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class SpectatorCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "spectator",
            CommandCategory.CORE,
            "Configure the Spectator feature",
            asList("on/off",
                        "whitelist add/del <player>", "whitelist list", "whitelist clear",
                        "entity list", "entity <entity>",
                        "chat on/off")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spectator").requires(Command::validateAccountOwner)
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.spectator.allowSpectator = getToggle(c, "toggle");
                if (!CONFIG.server.spectator.allowSpectator)
                    Proxy.getInstance().getSpectatorConnections()
                        .forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                c.getSource().getEmbedBuilder()
                    .title("Spectators " + (CONFIG.server.spectator.allowSpectator ? "On!" : "Off!"))
                    .color(Color.CYAN)
                    .description("Spectator Whitelist:\n " + whitelistToString());;
                return 1;
            }))
            .then(literal("whitelist")
                      .then(literal("add").then(argument("player", string()).executes(c -> {
                          final String playerName = StringArgumentType.getString(c, "player");
                          WHITELIST_MANAGER.addSpectatorWhitelistEntryByUsername(playerName)
                              .ifPresentOrElse(e ->
                                                   c.getSource().getEmbedBuilder()
                                                       .title("Added user: " + escape(e.username) + " To Spectator Whitelist")
                                                       .color(Color.CYAN)
                                                       .description("Spectator Whitelist:\n " + whitelistToString()),
                                               () -> c.getSource().getEmbedBuilder()
                                                   .title("Failed to add user: " + escape(playerName) + " to whitelist. Unable to lookup profile.")
                                                   .color(Color.RUBY)
                                                   .description("Spectator Whitelist:\n " + whitelistToString()));
                          return 1;
                      })))
                      .then(literal("del").then(argument("player", string()).executes(c -> {
                          final String playerName = StringArgumentType.getString(c, "player");
                          WHITELIST_MANAGER.removeSpectatorWhitelistEntryByUsername(playerName);
                          c.getSource().getEmbedBuilder()
                              .title("Removed user: " + escape(playerName) + " From Spectator Whitelist")
                              .color(Color.CYAN)
                              .description("Spectator Whitelist:\n " + whitelistToString());
                          WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                          return 1;
                      })))
                      .then(literal("clear").executes(c -> {
                          WHITELIST_MANAGER.clearSpectatorWhitelist();
                          c.getSource().getEmbedBuilder()
                              .title("Spectator Whitelist Cleared")
                              .color(Color.RUBY)
                              .description("Spectator Whitelist:\n " + whitelistToString());;
                          WHITELIST_MANAGER.kickNonWhitelistedPlayers();
                      }))
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbedBuilder()
                              .title("Spectator Whitelist")
                              .color(Color.CYAN)
                              .description("Spectator Whitelist:\n " + whitelistToString());
                      })))
            .then(literal("entity")
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbedBuilder()
                              .title("Entity List")
                              .description("Entity List: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()))
                              .color(Color.CYAN);
                      }))
                      .then(argument("entityID", string()).executes(c -> {
                          final String entityInput = StringArgumentType.getString(c, "entityID");
                          Optional<SpectatorEntity> spectatorEntity = SpectatorEntityRegistry.getSpectatorEntity(entityInput);
                          if (spectatorEntity.isPresent()) {
                              CONFIG.server.spectator.spectatorEntity = entityInput;
                              c.getSource().getEmbedBuilder()
                                  .title("Set Entity")
                                  .color(Color.CYAN);
                          } else {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Entity")
                                  .description("Entity List: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers()))
                                  .color(Color.RUBY);
                          }
                          return 1;
                      })))
            .then(literal("chat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.server.spectator.spectatorPublicChatEnabled = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("Spectator Chat " + (CONFIG.server.spectator.spectatorPublicChatEnabled ? "On!" : "Off!"))
                                .color(Color.CYAN)
                                .description("Spectator Whitelist:\n " + whitelistToString());
                            return 1;
                      })));
    }

    private String whitelistToString() {
        return CONFIG.server.spectator.whitelist.isEmpty()
                ? "Empty"
                : CONFIG.server.spectator.whitelist.stream()
                .map(mp -> escape(mp.username + " [[" + mp.uuid.toString() + "](" + mp.getNameMCLink() + ")]"))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator), false)
            .addField("Chat", toggleStr(CONFIG.server.spectator.spectatorPublicChatEnabled), false)
            .addField("Entity", CONFIG.server.spectator.spectatorEntity, false);
    }
}
