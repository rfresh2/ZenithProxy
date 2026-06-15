package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.DATABASE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class DatabaseCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("database")
            .category(CommandCategory.MANAGE)
            .description("""
            Configures the database module used by https://api.2b2t.vc

            This is disabled by default - no ZenithProxy users contribute or collect data
            """)
            .usageLines(
                "on/off",
                "host <host>",
                "port <port>",
                "username <username>",
                "password <password>",
                "redis address <address>",
                "redis username <username>",
                "redis password <password>",
                "queueWait on/off",
                "queueLength on/off",
                "publicChat on/off",
                "joinLeave on/off",
                "deathMessages on/off",
                "restarts on/off",
                "playerCount on/off",
                "tablist on/off",
                "playtime on/off",
                "time on/off"
            )
            .aliases("db")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("database")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.database.enabled = getToggle(c, "toggle");
                if (CONFIG.database.enabled) DATABASE.start();
                else DATABASE.stop();
                c.getSource().getEmbed()
                    .title("Databases " + toggleStrCaps(CONFIG.database.enabled));
                return OK;
            }))
            .then(literal("host").then(argument("hostArg", wordWithChars()).executes(c -> {
                CONFIG.database.host = getString(c , "hostArg");
                if (CONFIG.database.enabled) {
                    DATABASE.stop();
                    DATABASE.start();
                }
                c.getSource().getEmbed()
                    .title("Host Set");
            })))
            .then(literal("port").then(argument("portArg", integer(1, 65535)).executes(c -> {
                CONFIG.database.port = getInteger(c, "portArg");
                if (CONFIG.database.enabled) {
                    DATABASE.stop();
                    DATABASE.start();
                }
                c.getSource().getEmbed()
                    .title("Port Set");
            })))
            .then(literal("username").then(argument("usernameArg", wordWithChars()).executes(c -> {
                CONFIG.database.username = getString(c, "usernameArg");
                if (CONFIG.database.enabled) {
                    DATABASE.stop();
                    DATABASE.start();
                }
                c.getSource().getEmbed()
                    .title("Username Set");
            })))
            .then(literal("password").then(argument("passwordArg", wordWithChars()).executes(c -> {
                CONFIG.database.password = getString(c, "passwordArg");
                if (CONFIG.database.enabled) {
                    DATABASE.stop();
                    DATABASE.start();
                }
                c.getSource().setSensitiveInput(true);
                c.getSource().getEmbed()
                    .title("Password Set");
            })))
            .then(literal("redis")
                .then(literal("address").then(argument("redisAddress", wordWithChars()).executes(c -> {
                    CONFIG.database.lock.redisAddress = getString(c, "redisAddress");
                    if (CONFIG.database.enabled) {
                        DATABASE.stop();
                        DATABASE.start();
                    }
                    c.getSource().getEmbed()
                        .title("Redis Address Set");
                })))
                .then(literal("username").then(argument("redisUsername", wordWithChars()).executes(c -> {
                    CONFIG.database.lock.redisUsername = getString(c, "redisUsername");
                    if (CONFIG.database.enabled) {
                        DATABASE.stop();
                        DATABASE.start();
                    }
                    c.getSource().getEmbed()
                        .title("Redis Username Set");
                })))
                .then(literal("password").then(argument("redisPassword", wordWithChars()).executes(c -> {
                    CONFIG.database.lock.redisPassword = getString(c, "redisPassword");
                    if (CONFIG.database.enabled) {
                        DATABASE.stop();
                        DATABASE.start();
                    }
                    c.getSource().setSensitiveInput(true);
                    c.getSource().getEmbed()
                        .title("Redis Password Set");
                }))))
            .then(literal("queueWait")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.queueWaitEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.queueWaitEnabled) DATABASE.startQueueWaitDatabase();
                          else DATABASE.stopQueueWaitDatabase();
                          c.getSource().getEmbed()
                              .title("Queue Wait Database " + toggleStrCaps(CONFIG.database.queueWaitEnabled));
                          return OK;
                      })))
            .then(literal("queueLength")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.queueLengthEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.queueLengthEnabled) DATABASE.startQueueLengthDatabase();
                          else DATABASE.stopQueueLengthDatabase();
                          c.getSource().getEmbed()
                              .title("Queue Length Database " + toggleStrCaps(CONFIG.database.queueLengthEnabled));
                          return OK;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.chatsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.chatsEnabled) DATABASE.startChatsDatabase();
                          else DATABASE.stopChatsDatabase();
                          c.getSource().getEmbed()
                              .title("Public Chat Database " + toggleStrCaps(CONFIG.database.chatsEnabled));
                          return OK;
                      })))
            .then(literal("joinLeave")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.connectionsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.connectionsEnabled) DATABASE.startConnectionsDatabase();
                          else DATABASE.stopConnectionsDatabase();
                          c.getSource().getEmbed()
                              .title("Connections Database " + toggleStrCaps(CONFIG.database.connectionsEnabled));
                          return OK;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.deathsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.deathsEnabled) DATABASE.startDeathsDatabase();
                          else DATABASE.stopDeathsDatabase();
                          c.getSource().getEmbed()
                              .title("Death Messages Database " + toggleStrCaps(CONFIG.database.deathsEnabled));
                          return OK;
                      })))
            .then(literal("restarts")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.restartsEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.restartsEnabled) DATABASE.startRestartsDatabase();
                          else DATABASE.stopRestartsDatabase();
                          c.getSource().getEmbed()
                              .title("Restarts Database " + toggleStrCaps(CONFIG.database.restartsEnabled));
                          return OK;
                      })))
            .then(literal("playerCount")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.playerCountEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.playerCountEnabled) DATABASE.startPlayerCountDatabase();
                          else DATABASE.stopPlayerCountDatabase();
                          c.getSource().getEmbed()
                              .title("Player Count Database " + toggleStrCaps(CONFIG.database.playerCountEnabled));
                          return OK;
                      })))
            .then(literal("tablist")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.tablistEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.tablistEnabled) DATABASE.startTablistDatabase();
                          else DATABASE.stopTablistDatabase();
                          c.getSource().getEmbed()
                              .title("Tablist Database " + toggleStrCaps(CONFIG.database.tablistEnabled));
                          return OK;
                      })))
            .then(literal("playtime")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.database.playtimeEnabled = getToggle(c, "toggle");
                          if (CONFIG.database.playtimeEnabled) DATABASE.startPlaytimeDatabase();
                          else DATABASE.stopPlaytimeDatabase();
                          c.getSource().getEmbed()
                              .title("Playtime Database " + toggleStrCaps(CONFIG.database.playtimeEnabled));
                          return OK;
                      })))
            .then(literal("time")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.timeEnabled = getToggle(c, "toggle");
                            if (CONFIG.database.timeEnabled) DATABASE.startTimeDatabase();
                            else DATABASE.stopTimeDatabase();
                            c.getSource().getEmbed()
                                .title("Time Database " + toggleStrCaps(CONFIG.database.timeEnabled));
                            return OK;
                      })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Queue Wait", toggleStr(CONFIG.database.queueWaitEnabled), false)
            .addField("Queue Length", toggleStr(CONFIG.database.queueLengthEnabled), false)
            .addField("Public Chat", toggleStr(CONFIG.database.chatsEnabled), false)
            .addField("Join/Leave", toggleStr(CONFIG.database.connectionsEnabled), false)
            .addField("Death Messages", toggleStr(CONFIG.database.deathsEnabled), false)
            .addField("Restarts", toggleStr(CONFIG.database.restartsEnabled), false)
            .addField("Player Count", toggleStr(CONFIG.database.playerCountEnabled), false)
            .addField("Tablist", toggleStr(CONFIG.database.tablistEnabled), false)
            .addField("Playtime", toggleStr(CONFIG.database.playtimeEnabled), false)
            .addField("Time", toggleStr(CONFIG.database.timeEnabled), false)
            .primaryColor();
    }
}
