package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.entity.SpectatorEntity;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PLAYER_LISTS;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.command.util.CommandOutputHelper.playerListToString;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class SpectatorCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "spectator",
            CommandCategory.CORE,
            """
            Configures the Spectator feature.
            
            The spectator whitelist only allows players to join as spectators.
            Players who are regular whitelisted (i.e. with the `whitelist` command) can always join as spectators regardless.
            
            Spectator entities control what entity is used to represent spectators in-game.
            """,
            asList(
                "on/off",
                "whitelist add/del <player>",
                "whitelist list",
                "whitelist clear",
                "entity list",
                "entity <entity>",
                "chat on/off"
            )
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
                c.getSource().getEmbed()
                    .title("Spectators " + toggleStrCaps(CONFIG.server.spectator.allowSpectator))
                    .primaryColor()
                    .description(spectatorWhitelist());
                return OK;
            }))
            .then(literal("whitelist")
                      .then(literal("add").then(argument("player", string()).executes(c -> {
                          final String playerName = StringArgumentType.getString(c, "player");
                          PLAYER_LISTS.getSpectatorWhitelist().add(playerName)
                              .ifPresentOrElse(e ->
                                                   c.getSource().getEmbed()
                                                       .title("Added user: " + escape(e.getUsername()) + " To Spectator Whitelist")
                                                       .primaryColor()
                                                       .description(spectatorWhitelist()),
                                               () -> c.getSource().getEmbed()
                                                   .title("Failed to add user: " + escape(playerName) + " to whitelist. Unable to lookup profile.")
                                                   .errorColor()
                                                   .description(spectatorWhitelist()));
                          return 1;
                      })))
                      .then(literal("del").then(argument("player", string()).executes(c -> {
                          final String playerName = StringArgumentType.getString(c, "player");
                          PLAYER_LISTS.getSpectatorWhitelist().remove(playerName);
                          c.getSource().getEmbed()
                              .title("Removed user: " + escape(playerName) + " From Spectator Whitelist")
                              .primaryColor()
                              .description(spectatorWhitelist());
                          Proxy.getInstance().kickNonWhitelistedPlayers();
                          return 1;
                      })))
                      .then(literal("clear").executes(c -> {
                          PLAYER_LISTS.getSpectatorWhitelist().clear();
                          c.getSource().getEmbed()
                              .title("Spectator Whitelist Cleared")
                              .errorColor()
                              .description(spectatorWhitelist());
                          Proxy.getInstance().kickNonWhitelistedPlayers();
                      }))
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbed()
                              .title("Spectator Whitelist")
                              .primaryColor()
                              .description(spectatorWhitelist());
                      })))
            .then(literal("entity")
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbed()
                              .title("Entity List")
                              .description(entityList())
                              .primaryColor();
                      }))
                      .then(argument("entityID", string()).executes(c -> {
                          final String entityInput = StringArgumentType.getString(c, "entityID");
                          Optional<SpectatorEntity> spectatorEntity = SpectatorEntityRegistry.getSpectatorEntity(entityInput);
                          if (spectatorEntity.isPresent()) {
                              CONFIG.server.spectator.spectatorEntity = entityInput;
                              c.getSource().getEmbed()
                                  .title("Set Entity")
                                  .primaryColor();
                          } else {
                              c.getSource().getEmbed()
                                  .title("Invalid Entity")
                                  .description(entityList())
                                  .errorColor();
                          }
                          return 1;
                      })))
            .then(literal("chat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.server.spectator.spectatorPublicChatEnabled = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Spectator Chat " + toggleStrCaps(CONFIG.server.spectator.spectatorPublicChatEnabled))
                                .primaryColor()
                                .description(spectatorWhitelist());
                            return 1;
                      })));
    }

    private String spectatorWhitelist() {
        return "**Spectator Whitelist**\n" + playerListToString(PLAYER_LISTS.getSpectatorWhitelist());
    }

    private String entityList() {
        return "**Entity List**\n" + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers());
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator), false)
            .addField("Chat", toggleStr(CONFIG.server.spectator.spectatorPublicChatEnabled), false)
            .addField("Entity", CONFIG.server.spectator.spectatorEntity, false);
    }
}
