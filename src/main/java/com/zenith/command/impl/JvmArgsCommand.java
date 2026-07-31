package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.LAUNCH_CONFIG;
import static com.zenith.Globals.saveLaunchConfig;
import static com.zenith.discord.DiscordBot.escape;

public class JvmArgsCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("jvmArgs")
            .category(CommandCategory.MANAGE)
            .description("""
              Configures ZenithProxy's JVM arguments used by the launcher.

              By default, this is empty and a set of default JVM arguments are used.

              The primary arg to configure is `-Xmx` which sets the maximum memory heap size.

              The default `-Xmx` used by the launcher depends on the `java` or `linux` release channel:
              * `java`: 300M
              * `linux`: 225M

              You should only need to increase this if the server view distance is > 15.

              Be warned, changing this setting can cause ZenithProxy to be unable to restart. You will need to manually
              edit the `launch_config.json` to fix this if that happens.
              """)
            .usageLines(
                "reset",
                "get",
                "setXmx <megabytes>",
                "set <args>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("jvmArgs").requires(Command::validateAccountOwner)
            .then(literal("reset").executes(c -> {
                LAUNCH_CONFIG.custom_jvm_args = null;
                saveLaunchConfig();
                c.getSource().getEmbed()
                    .title("JVM Args Reset")
                    .description("Restart ZenithProxy for changes to take effect: `restart`")
                    .primaryColor();
            }))
            .then(literal("get").executes(c -> {
                var args = LAUNCH_CONFIG.custom_jvm_args;
                c.getSource().getEmbed()
                    .title("JVM Args")
                    .description(args == null ? "(default)" : escape(args))
                    .primaryColor();
            }))
            .then(literal("setXmx").then(argument("mb", integer(100)).executes(c -> {
                int mb = getInteger(c, "mb");
                LAUNCH_CONFIG.custom_jvm_args = "-Xmx" + mb + "M";
                saveLaunchConfig();
                c.getSource().getEmbed()
                    .title("JVM Args Set")
                    .description("**JVM Args:**\n" + escape(LAUNCH_CONFIG.custom_jvm_args) + "\nRestart ZenithProxy for changes to take effect: `restart`")
                    .primaryColor();
                return OK;
            })))
            .then(literal("set").then(argument("args", greedyString()).executes(c -> {
                LAUNCH_CONFIG.custom_jvm_args = getString(c, "args");
                saveLaunchConfig();
                c.getSource().getEmbed()
                    .title("JVM Args Set")
                    .description("**JVM Args:**\n" + escape(LAUNCH_CONFIG.custom_jvm_args) + "\nRestart ZenithProxy for changes to take effect: `restart`");
                return OK;
            })));
    }
}
