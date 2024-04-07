package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DatabaseCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("database",
                                 CommandCategory.MANAGE,
                                 "Configures what 2b2t server data is collected by the proxy. No database logs personal data.",
                                 asList(
                                     "on/off",
                                     "queueWait on/off",
                                     "queueLength on/off",
                                     "publicChat on/off",
                                     "joinLeave on/off",
                                     "deathMessages on/off",
                                     "restarts on/off",
                                     "playerCount on/off",
                                     "tablist on/off"
                                 ),
                                 asList("db")
        );
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
            .then(literal("queueWait")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.queueWait.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.queueWait.enabled) DATABASE.startQueueWaitDatabase();
                            else DATABASE.stopQueueWaitDatabase();
                            c.getSource().getEmbed()
                                .title("Queue Wait Database " + toggleStrCaps(CONFIG.database.queueWait.enabled));
                            return OK;
                      })))
            .then(literal("queueLength")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.queueLength.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.queueLength.enabled) DATABASE.startQueueLengthDatabase();
                            else DATABASE.stopQueueLengthDatabase();
                            c.getSource().getEmbed()
                                .title("Queue Length Database " + toggleStrCaps(CONFIG.database.queueLength.enabled));
                            return OK;
                      })))
            .then(literal("publicChat")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.chats.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.chats.enabled) DATABASE.startChatsDatabase();
                            else DATABASE.stopChatsDatabase();
                            c.getSource().getEmbed()
                                .title("Public Chat Database " + toggleStrCaps(CONFIG.database.chats.enabled));
                            return OK;
                      })))
            .then(literal("joinLeave")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.connections.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.connections.enabled) DATABASE.startConnectionsDatabase();
                            else DATABASE.stopConnectionsDatabase();
                            c.getSource().getEmbed()
                                .title("Connections Database " + toggleStrCaps(CONFIG.database.connections.enabled));
                            return OK;
                      })))
            .then(literal("deathMessages")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.database.deaths.enabled = getToggle(c, "toggle");
                            if (CONFIG.database.deaths.enabled) DATABASE.startDeathsDatabase();
                            else DATABASE.stopDeathsDatabase();
                            c.getSource().getEmbed()
                                .title("Death Messages Database " + toggleStrCaps(CONFIG.database.deaths.enabled));
                            return OK;
                      })))
            .then(literal("restarts")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.restarts.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.restarts.enabled) DATABASE.startRestartsDatabase();
                                else DATABASE.stopRestartsDatabase();
                                c.getSource().getEmbed()
                                    .title("Restarts Database " + toggleStrCaps(CONFIG.database.restarts.enabled));
                                return OK;
                        })))
            .then(literal("playerCount")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.playerCount.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.playerCount.enabled) DATABASE.startPlayerCountDatabase();
                                else DATABASE.stopPlayerCountDatabase();
                                c.getSource().getEmbed()
                                    .title("Player Count Database " + toggleStrCaps(CONFIG.database.playerCount.enabled));
                                return OK;
                        })))
            .then(literal("tablist")
                        .then(argument("toggle", toggle()).executes(c -> {
                                CONFIG.database.tablist.enabled = getToggle(c, "toggle");
                                if (CONFIG.database.tablist.enabled) DATABASE.startTablistDatabase();
                                else DATABASE.stopTablistDatabase();
                                c.getSource().getEmbed()
                                    .title("Tablist Database " + toggleStrCaps(CONFIG.database.tablist.enabled));
                                return OK;
                        })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("Queue Wait", toggleStr(CONFIG.database.queueWait.enabled), false)
            .addField("Queue Length", toggleStr(CONFIG.database.queueLength.enabled), false)
            .addField("Public Chat", toggleStr(CONFIG.database.chats.enabled), false)
            .addField("Join/Leave", toggleStr(CONFIG.database.connections.enabled), false)
            .addField("Death Messages", toggleStr(CONFIG.database.deaths.enabled), false)
            .addField("Restarts", toggleStr(CONFIG.database.restarts.enabled), false)
            .addField("Player Count", toggleStr(CONFIG.database.playerCount.enabled), false)
            .addField("Tablist", toggleStr(CONFIG.database.tablist.enabled), false)
            .primaryColor();
    }
}
