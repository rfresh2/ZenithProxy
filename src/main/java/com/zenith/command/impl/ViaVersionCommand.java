package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class ViaVersionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "via",
            "Configure ViaVersion",
            asList(
                "on/off",
                "autoConfig on/off",
                "version <MC version>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("via")
            .then(literal("on").executes(c -> {
                CONFIG.client.viaversion.enabled = true;
                defaultEmbedPopulate(c.getSource().getEmbedBuilder()
                    .title("ViaVersion Enabled!"));
            }))
            .then(literal("off").executes(c -> {
                CONFIG.client.viaversion.enabled = false;
                defaultEmbedPopulate(c.getSource().getEmbedBuilder()
                    .title("ViaVersion Disabled!"));
            }))
            .then(literal("autoconfig")
                      .then(literal("on").executes(c -> {
                          CONFIG.client.viaversion.autoProtocolVersion = true;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder()
                              .title("ViaVersion AutoConfig Enabled!"));
                      }))
                      .then(literal("off").executes(c -> {
                          CONFIG.client.viaversion.autoProtocolVersion = false;
                          defaultEmbedPopulate(c.getSource().getEmbedBuilder()
                              .title("ViaVersion AutoConfig Disabled!"));
                      })))
            .then(literal("version")
                      .then(argument("version", wordWithChars()).executes(c -> {
                          final String version = StringArgumentType.getString(c, "version");
                          ProtocolVersion closest = ProtocolVersion.getClosest(version);
                          if (closest == null) {
                              c.getSource().getEmbedBuilder()
                                  .title("Invalid Version!")
                                  .description("Please select a valid version. Example: 1.19.4")
                                  .color(Color.RED);
                          } else {
                              CONFIG.client.viaversion.protocolVersion = closest.getVersion();
                              defaultEmbedPopulate(c.getSource().getEmbedBuilder()
                                  .title("ViaVersion Version Updated!"));
                          }
                          return 1;
                      })));
    }

    private EmbedCreateSpec.Builder defaultEmbedPopulate(final EmbedCreateSpec.Builder embedBuilder) {
        return embedBuilder
            .addField("ViaVersion", CONFIG.client.viaversion.enabled ? "on" : "off", false)
            .addField("AutoConfig", CONFIG.client.viaversion.autoProtocolVersion ? "on" : "off", false)
            .addField("Version", ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion).getName(), false)
            .color(Color.CYAN);
    }}
