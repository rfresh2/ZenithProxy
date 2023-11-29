package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.module.Module;
import com.zenith.module.impl.AutoDisconnect;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE_MANAGER;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoDisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "autoDisconnect",
            CommandCategory.MODULE,
            "Configures the AutoDisconnect feature",
            asList(
                        "on/off",
                        "health <integer>",
                        "cancelAutoReconnect on/off",
                        "autoClientDisconnect on/off",
                        "thunder on/off"),
            asList("autoLog")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoDisconnect")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.utility.actions.autoDisconnect.enabled = getToggle(c, "toggle");
                MODULE_MANAGER.getModule(AutoDisconnect.class).ifPresent(Module::syncEnabledFromConfig);
                c.getSource().getEmbedBuilder()
                    .title("AutoDisconnect " + (CONFIG.client.extra.utility.actions.autoDisconnect.enabled ? "On!" : "Off!"));
                return 1;
            }))
            .then(literal("health").then(argument("healthLevel", integer()).executes(c -> {
                CONFIG.client.extra.utility.actions.autoDisconnect.health = IntegerArgumentType.getInteger(c, "healthLevel");
                c.getSource().getEmbedBuilder()
                    .title("AutoDisconnect Health Updated!");
                return 1;
            })))
            .then(literal("cancelAutoReconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("AutoDisconnect CancelAutoReconnect " + (CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("autoClientDisconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("AutoDisconnect AutoClientDisconnect " + (CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect ? "On!" : "Off!"));
                            return 1;
                      })))
            .then(literal("thunder")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = getToggle(c, "toggle");
                            c.getSource().getEmbedBuilder()
                                .title("AutoDisconnect Thunder " + (CONFIG.client.extra.utility.actions.autoDisconnect.thunder ? "On!" : "Off!"));
                            return 1;
                      })));
    }

    @Override
    public void postPopulate(final EmbedCreateSpec.Builder builder) {
        builder
            .addField("AutoDisconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.enabled), false)
            .addField("Health", "" + CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
            .addField("CancelAutoReconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect), false)
            .addField("AutoClientDisconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect), false)
            .addField("Thunder", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.thunder), false)
            .color(Color.CYAN);
    }
}
