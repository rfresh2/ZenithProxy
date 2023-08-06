package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
                "debug",
                "Debug settings for developers",
                asList(
                        "autoConnect on/off",
                        "packetLog on/off"
                        // todo: packet filter setting
                ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
                .then(literal("autoconnect")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.autoConnect = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Auto Connect On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.autoConnect = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Auto Connect Off!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("packetlog")
                        .then(literal("on").executes(c -> {
                            CONFIG.debug.packet.received = true;
                            CONFIG.debug.packet.receivedBody = true;
                            CONFIG.debug.packet.preSent = false;
                            CONFIG.debug.packet.postSent = true;
                            CONFIG.debug.packet.postSentBody = true;
                            c.getSource().getEmbedBuilder()
                                    .title("Packet Log On!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.debug.packet.received = false;
                            CONFIG.debug.packet.postSent = false;
                            c.getSource().getEmbedBuilder()
                                    .title("Packet Log Off!")
                                    .color(Color.CYAN);
                        })));
    }
}
