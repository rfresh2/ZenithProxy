package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.Arrays;

import static com.zenith.util.Constants.CONFIG;

public class ExtraChatCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args("extraChat", "Extra chat commands", Arrays.asList(
                "hideChat on/off",
                "hideWhispers on/off",
                "hideDeathMessages on/off",
                "showConnectionMessages on/off"
        ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("extrachat")
                .then(literal("hidechat")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.chat.hideChat = true;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Chat hidden!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.chat.hideChat = false;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Chat shown!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("hidewhispers")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.chat.hideWhispers = true;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Whispers hidden!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.chat.hideWhispers = false;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Whispers shown!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("hidedeathmessages")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.chat.hideDeathMessages = true;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Death messages hidden!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.chat.hideDeathMessages = false;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Death messages shown!")
                                    .color(Color.CYAN);
                        })))
                .then(literal("showconnectionmessages")
                        .then(literal("on").executes(c -> {
                            CONFIG.client.extra.chat.showConnectionMessages = true;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Connection messages shown!")
                                    .color(Color.CYAN);
                        }))
                        .then(literal("off").executes(c -> {
                            CONFIG.client.extra.chat.showConnectionMessages = false;
                            addStatus(c.getSource().getEmbedBuilder())
                                    .title("Connection messages hidden!")
                                    .color(Color.CYAN);
                        })));
    }

    public EmbedCreateSpec.Builder addStatus(final EmbedCreateSpec.Builder builder) {
        return builder
                .addField("Hide chat", CONFIG.client.extra.chat.hideChat ? "on" : "off", true)
                .addField("Hide whispers", CONFIG.client.extra.chat.hideWhispers ? "on" : "off", true)
                .addField("Hide death messages", CONFIG.client.extra.chat.hideDeathMessages ? "on" : "off", true)
                .addField("Show connection messages", CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off", true);
    }
}
