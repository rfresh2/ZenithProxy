package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ViaVersionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "via",
            CommandCategory.MODULE,
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
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.viaversion.enabled = getToggle(c, "toggle");
                c.getSource().getEmbedBuilder()
                    .title("ViaVersion " + (CONFIG.client.viaversion.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("autoconfig")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.viaversion.autoProtocolVersion = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("ViaVersion AutoConfig " + (CONFIG.client.viaversion.autoProtocolVersion ? "On!" : "Off!"));
                            return 1;
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
                              c.getSource().getEmbedBuilder()
                                  .title("ViaVersion Version Updated!");
                          }
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder embedBuilder) {
        embedBuilder
            .addField("ViaVersion", CONFIG.client.viaversion.enabled ? "on" : "off", false)
            .addField("AutoConfig", CONFIG.client.viaversion.autoProtocolVersion ? "on" : "off", false)
            .addField("Version", ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion).getName(), false)
            .color(Color.CYAN);
    }}
