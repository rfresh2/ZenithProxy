package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.entity.SpectatorEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.PLAYER_LISTS;
import static com.zenith.command.api.CommandOutputHelper.playerListToString;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;

public class SpectatorCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("spectator")
            .category(CommandCategory.CORE)
            .description("""
            Configures the Spectator feature.
            
            The spectator whitelist only allows players to join as spectators.
            Players who are regular whitelisted (i.e. with the `whitelist` command) can always join as spectators regardless.
            
            Spectator entities control what entity is used to represent spectators in-game.
            
            Full commands allow spectators access to all standard ZenithProxy commands like `connect`, `disconnect`, etc.
            If this is disabled, spectators only have access to a limited set of core commands.
            """)
            .usageLines(
                "on/off",
                "whitelist add/del <player>",
                "whitelist addAll <player 1>,<player 2>...",
                "whitelist list",
                "whitelist clear",
                "entity list",
                "entity <entity>",
                "chat on/off",
                "playerCamOnJoin on/off",
                "fullCommands on/off",
                "fullCommands slashCommands on/off",
                "fullCommands requireRegularWhitelist on/off"
            )
            .build();
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
                })))
                .then(literal("addAll").then(argument("playerList", greedyString()).executes(c -> {
                    String playerList = getString(c, "playerList");
                    String[] split = playerList.split(",");
                    if (split.length == 0) {
                        c.getSource().getEmbed()
                            .title("Invalid Input")
                            .description("Each player name must be delimited by `,`");
                        return ERROR;
                    }
                    List<String> addErrors = new ArrayList<>();
                    for (var player : split) {
                        if (PLAYER_LISTS.getSpectatorWhitelist().add(player).isEmpty()) {
                            addErrors.add(player);
                        }
                    }
                    c.getSource().getEmbed()
                        .title("Added Players")
                        .addField("Added Player Count", split.length - addErrors.size());
                    if (!addErrors.isEmpty()) {
                        c.getSource().getEmbed()
                            .description("Failed adding " + addErrors.size() + " players: " + String.join(", ", addErrors));
                    }
                    return OK;
                })))
                .then(literal("del").then(argument("player", string()).executes(c -> {
                    final String playerName = StringArgumentType.getString(c, "player");
                    PLAYER_LISTS.getSpectatorWhitelist().remove(playerName);
                    c.getSource().getEmbed()
                        .title("Removed user: " + escape(playerName) + " From Spectator Whitelist")
                        .primaryColor()
                        .description(spectatorWhitelist());
                    Proxy.getInstance().kickNonWhitelistedPlayers();
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
                .then(argument("entityID", enumStrings(SpectatorEntityRegistry.getEntityIdentifiers())).executes(c -> {
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
                })))
            .then(literal("chat")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.spectator.spectatorPublicChatEnabled = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Spectator Chat " + toggleStrCaps(CONFIG.server.spectator.spectatorPublicChatEnabled))
                        .primaryColor()
                        .description(spectatorWhitelist());
                })))
            .then(literal("playerCamOnJoin").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.spectator.playerCamOnJoin = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Player Cam On Join " + toggleStrCaps(CONFIG.server.spectator.playerCamOnJoin))
                    .primaryColor();
            })))
            .then(literal("fullCommands").requires(Command::validateAccountOwner)
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.spectator.fullCommandsEnabled = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Full Spectator Commands " + toggleStrCaps(CONFIG.server.spectator.fullCommandsEnabled))
                        .primaryColor();
                }))
                .then(literal("slashCommands").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.spectator.fullCommandsAcceptSlashCommands = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Full Spectator Commands Accept Slash Commands " + toggleStrCaps(CONFIG.server.spectator.fullCommandsAcceptSlashCommands))
                        .primaryColor();
                })))
                .then(literal("requireRegularWhitelist").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.server.spectator.fullCommandsRequireRegularWhitelist = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Full Spectator Commands Require Regular Whitelist " + toggleStrCaps(CONFIG.server.spectator.fullCommandsRequireRegularWhitelist))
                        .primaryColor();
                }))));
    }

    private String spectatorWhitelist() {
        return "**Spectator Whitelist**\n" + playerListToString(PLAYER_LISTS.getSpectatorWhitelist());
    }

    private String entityList() {
        return "**Entity List**\n" + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers());
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Spectators", toggleStr(CONFIG.server.spectator.allowSpectator))
            .addField("Chat", toggleStr(CONFIG.server.spectator.spectatorPublicChatEnabled))
            .addField("Entity", CONFIG.server.spectator.spectatorEntity)
            .addField("PlayerCam On Join", toggleStr(CONFIG.server.spectator.playerCamOnJoin))
            .addField("Full Commands", toggleStr(CONFIG.server.spectator.fullCommandsEnabled))
            .addField("Full Commands Slash Commands", toggleStr(CONFIG.server.spectator.fullCommandsAcceptSlashCommands))
            .addField("Full Commands Require Regular Whitelist", toggleStr(CONFIG.server.spectator.fullCommandsRequireRegularWhitelist));
    }
}
