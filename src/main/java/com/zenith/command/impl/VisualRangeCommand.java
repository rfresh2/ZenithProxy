package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.VisualRange;
import com.zenith.util.ChatUtil;
import com.zenith.util.config.Config;
import org.geysermc.mcprotocollib.auth.GameProfile;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.discord.DiscordBot.escape;

public class VisualRangeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("visualRange")
            .category(CommandCategory.MODULE)
            .description("""
            Configure the VisualRange notification feature.

            Alerts are sent both in the terminal and in discord, with optional discord mentions.

            `replayRecording` settings will start recording when players enter your visual range and stop
            when players leave, after the set cooldown.

            `enemy` mode will only record players who are not on your friends list.
            `all` mode will record all players, regardless of being on the friends list.

            To add players to the friends list see the `friends` command.
            """)
            .usageLines(
                "on/off",
                "list",
                "enter on/off",
                "enter mention on/off",
                "enter whisper on/off",
                "enter whisper message <message>",
                "enter whisper cooldown <seconds>",
                "enter whisper command <command>",
                "enter whisper whilePlayerConnected on/off",
                "leave on/off",
                "logout on/off",
                "ignoreFriends on/off",
                "replayRecording on/off",
                "replayRecording mode <enemy/all>",
                "replayRecording cooldown <minutes>"
            )
            .aliases("vr")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("visualRange")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.visualRange.enabled = getToggle(c, "toggle");
                MODULE.get(VisualRange.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("VisualRange " + toggleStrCaps(CONFIG.client.extra.visualRange.enabled));
                return OK;
            }))
            .then(literal("list").executes(c -> {
                var players = CACHE.getEntityCache().getEntities().values().stream().filter(e -> e instanceof EntityPlayer).map(e -> (EntityPlayer) e).toList();
                var friends = new ArrayList<GameProfile>();
                var nonFriends = new ArrayList<GameProfile>();
                for (EntityPlayer p : players) {
                    if (p.isSelfPlayer()) continue;
                    var playerEntry = CACHE.getTabListCache().get(p.getUuid());
                    if (playerEntry.isEmpty()) {
                        DEFAULT_LOG.warn("Failed to find player entry for {}", p.getUuid());
                        continue;
                    }
                    if (PLAYER_LISTS.getFriendsList().contains(playerEntry.get().getProfile()) || PLAYER_LISTS.getWhitelist().contains(playerEntry.get().getProfile())) {
                        friends.add(playerEntry.get().getProfile());
                    } else {
                        nonFriends.add(playerEntry.get().getProfile());
                    }
                }
                if (friends.isEmpty() && nonFriends.isEmpty()) {
                    c.getSource().getEmbed()
                        .title("VisualRange Players")
                        .description("No players in visual range")
                        .primaryColor();
                    return;
                }
                c.getSource().getEmbed()
                    .title("VisualRange Players")
                    .description("**Friends/Whitelisted Players**\n"
                        + friends.stream().map(GameProfile::getName).collect(Collectors.joining("\n"))
                        + "\n\n"
                        + "**Non-Friends/Non-Whitelisted Players**\n"
                        + nonFriends.stream().map(GameProfile::getName).collect(Collectors.joining("\n"))
                    )
                    .primaryColor();
            }))
            .then(literal("enter")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.enterAlert = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("VisualRange Enter Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.enterAlert));
                    return OK;
                }))
                .then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.enterAlertMention = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("VisualRange Enter Mentions " + toggleStrCaps(CONFIG.client.extra.visualRange.enterAlertMention));
                    return OK;
                })))
                .then(literal("whisper")
                    .then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.client.extra.visualRange.enterWhisper = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("VisualRange Enter Whisper " + toggleStrCaps(CONFIG.client.extra.visualRange.enterWhisper));
                        return OK;
                    }))
                    .then(literal("message").then(argument("message", greedyString()).executes(c -> {
                        var msg = getString(c, "message");
                        CONFIG.client.extra.visualRange.enterWhisperMessage = ChatUtil.sanitizeChatMessage(msg.substring(0, Math.min(msg.length(), 236)));
                        c.getSource().getEmbed()
                            .title("VisualRange Enter Whisper Message Set");
                        return OK;
                    })))
                    .then(literal("cooldown").then(argument("seconds", integer(0)).executes(c -> {
                        CONFIG.client.extra.visualRange.enterWhisperCooldownSeconds = getInteger(c, "seconds");
                        c.getSource().getEmbed()
                            .title("VisualRange Enter Whisper Cooldown Set");
                        return OK;
                    })))
                    .then(literal("whilePlayerConnected").then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.client.extra.visualRange.enterWhisperWhilePlayerConnected = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("VisualRange Enter Whisper While Player Connected " + toggleStrCaps(CONFIG.client.extra.visualRange.enterWhisperWhilePlayerConnected));
                    })))))
            .then(literal("ignoreFriends")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.ignoreFriends = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Ignore Friends " + toggleStrCaps(CONFIG.client.extra.visualRange.ignoreFriends));
                    return OK;
                })))
            .then(literal("leave")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.leaveAlert = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Leave Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.leaveAlert));
                    return OK;
                })))
            .then(literal("logout")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.logoutAlert = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Logout Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.logoutAlert));
                    return OK;
                })))
            .then(literal("replayRecording")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.visualRange.replayRecording = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Replay Recording " + toggleStrCaps(CONFIG.client.extra.visualRange.replayRecording));
                    return OK;
                }))
                .then(literal("mode").then(argument("modeArg", enumStrings("enemy", "all")).executes(c -> {
                    var arg = getString(c, "modeArg");
                    var mode = switch (arg) {
                        case "enemy" -> Config.Client.Extra.VisualRange.ReplayRecordingMode.ENEMY;
                        case "all" -> Config.Client.Extra.VisualRange.ReplayRecordingMode.ALL;
                        default -> null;
                    };
                    if (mode == null) {
                        c.getSource().getEmbed()
                            .title("Invalid Replay Recording Mode");
                        return ERROR;
                    } else {
                        CONFIG.client.extra.visualRange.replayRecordingMode = mode;
                        c.getSource().getEmbed()
                            .title("Replay Recording Mode Set");
                        return OK;
                    }
                })))
                .then(literal("cooldown").then(argument("minutes", integer(0)).executes(c -> {
                    CONFIG.client.extra.visualRange.replayRecordingCooldownMins = getInteger(c, "minutes");
                    c.getSource().getEmbed()
                        .title("Enemy Replay Recording Cooldown Set");
                    return OK;
                }))));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("VisualRange", toggleStr(CONFIG.client.extra.visualRange.enabled))
            .addField("Enter Alerts", toggleStr(CONFIG.client.extra.visualRange.enterAlert))
            .addField("Enter Mentions", toggleStr(CONFIG.client.extra.visualRange.enterAlertMention))
            .addField("Enter Whisper", toggleStr(CONFIG.client.extra.visualRange.enterWhisper))
            .addField("Enter Whisper Message", escape(CONFIG.client.extra.visualRange.enterWhisperMessage))
            .addField("Enter Whisper Cooldown", CONFIG.client.extra.visualRange.enterWhisperCooldownSeconds + "s")
            .addField("Enter Whisper While Player Connected", toggleStr(CONFIG.client.extra.visualRange.enterWhisperWhilePlayerConnected))
            .addField("Ignore Friends", toggleStr(CONFIG.client.extra.visualRange.ignoreFriends))
            .addField("Leave Alerts", toggleStr(CONFIG.client.extra.visualRange.leaveAlert))
            .addField("Logout Alerts", toggleStr(CONFIG.client.extra.visualRange.logoutAlert))
            .addField("Replay Recording", toggleStr(CONFIG.client.extra.visualRange.replayRecording))
            .addField("Replay Recording Mode", CONFIG.client.extra.visualRange.replayRecordingMode.toString().toLowerCase())
            .addField("Replay Recording Cooldown", CONFIG.client.extra.visualRange.replayRecordingCooldownMins)
            .primaryColor();
    }
}
