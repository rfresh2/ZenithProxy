package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.List;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.DATABASE_MANAGER;
import static java.util.Arrays.asList;

public class DatabaseCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full("database",
                "Configures what 2b2t server data is collected by the proxy. No database logs personal data.",
                asList(
                        "queueWait on/off",
                        "queueLength on/off",
                        "publicChat on/off",
                        "joinLeave on/off",
                        "deathMessages on/off",
                        "restarts on/off",
                        "playerCount on/off"
                ),
                aliases());
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("database")
                .then(literal("queuewait")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.queueWait.enabled = true;
                            DATABASE_MANAGER.startQueueWaitDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Queue Wait Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.queueWait.enabled = false;
                            DATABASE_MANAGER.stopQueueWaitDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Queue Wait Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("queuelength")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.queueLength.enabled = true;
                            DATABASE_MANAGER.startQueueLengthDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Queue Length Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.queueLength.enabled = false;
                            DATABASE_MANAGER.stopQueueLengthDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Queue Length Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("publicchat")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.chats.enabled = true;
                            DATABASE_MANAGER.startChatsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Public Chat Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.chats.enabled = false;
                            DATABASE_MANAGER.stopChatsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Public Chat Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("joinleave")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.connections.enabled = true;
                            DATABASE_MANAGER.startConnectionsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Join/Leave Connections Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.connections.enabled = false;
                            DATABASE_MANAGER.stopConnectionsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Join/Leave Connections Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("deathmessages")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.deaths.enabled = true;
                            DATABASE_MANAGER.startDeathsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Death Messages Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.deaths.enabled = false;
                            DATABASE_MANAGER.stopDeathsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Death Messages Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("restarts")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.restarts.enabled = true;
                            DATABASE_MANAGER.startRestartsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Restarts Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.restarts.enabled = false;
                            DATABASE_MANAGER.stopRestartsDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Restarts Database Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("playercount")
                        .then(literal("on").executes(c -> {
                            CONFIG.database.playerCount.enabled = true;
                            DATABASE_MANAGER.startPlayerCountDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Player Count Database On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.database.playerCount.enabled = false;
                            DATABASE_MANAGER.stopPlayerCountDatabase();
                            c.getSource().getEmbedBuilder()
                                    .title("Player Count Database Off!")
                                    .color(Color.CYAN);
                        })));
    }

    @Override
    public List<String> aliases() {
        return asList("db");
    }
}
