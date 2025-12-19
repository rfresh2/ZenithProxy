package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

public class ShutdownCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("shutdown")
            .category(CommandCategory.MANAGE)
            .description("""
                Shuts down ZenithProxy, without letting the launcher restart it.
                """)
            .aliases("exit")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("shutdown").requires(Command::validateAccountOwner).executes(c -> {
            c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                .title("Shutting down...")
                .errorColor()
            );
            c.getSource().setNoOutput(true);
            Proxy.getInstance().stop(false);
        });
    }
}
