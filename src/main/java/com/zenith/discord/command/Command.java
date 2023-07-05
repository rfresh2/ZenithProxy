package com.zenith.discord.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.zenith.util.Constants.CONFIG;

public abstract class Command {
    public static <T> RequiredArgumentBuilder<CommandContext, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean validateAccountOwner(final CommandContext context) {
        if (context.getCommandSource() != CommandSource.DISCORD) return true;
        final DiscordCommandContext discordCommandContext = (DiscordCommandContext) context;
        final MessageCreateEvent event = discordCommandContext.getMessageCreateEvent();
        final boolean hasAccountOwnerRole = event.getMember()
                .orElseThrow(() -> new RuntimeException("Message does not have a valid member"))
                .getRoleIds()
                .stream()
                .map(Snowflake::asString)
                .anyMatch(roleId -> roleId.equals(CONFIG.discord.accountOwnerRoleId));
        if (!hasAccountOwnerRole) {
            context.getEmbedBuilder()
                    .title("Not Authorized!")
                    .color(Color.RUBY)
                    .addField("Error",
                            "User: " + event.getMember().map(User::getTag).orElse("Unknown")
                                    + " is not authorized to execute this command! Contact the account owner", true)
                    .build();
            return false;
        }
        return true;
    }

    public static CaseInsensitiveLiteralArgumentBuilder<CommandContext> literal(String literal) {
        return CaseInsensitiveLiteralArgumentBuilder.literal(literal);
    }

    public static CaseInsensitiveLiteralArgumentBuilder<CommandContext> literal(String literal, Function<CommandContext, Void> errorHandler) {
        return literal(literal).withErrorHandler(errorHandler);
    }

    /**
     * Required. Registers {@link CommandUsage}
     */
    public abstract CommandUsage commandUsage();

    /**
     * Required. Register a {@link #command}
     */
    public abstract LiteralArgumentBuilder<CommandContext> register();

    /**
     * Optional override to register command aliases.
     * Also check these are set in {@link #commandUsage()}
     * todo: auto-set these in commandUsage
     */
    public List<String> aliases() {
        return Collections.emptyList();
    }

    public CaseInsensitiveLiteralArgumentBuilder<CommandContext> command(String literal) {
        return literal(literal).withErrorHandler(this::usageErrorHandler);
    }

    /**
     * Workaround for no-arg redirect nodes
     * see https://github.com/Mojang/brigadier/issues/46
     * 4 years and no official fix T.T
     */
    public LiteralArgumentBuilder<CommandContext> redirect(String literal, final CommandNode<CommandContext> destination) {
        final LiteralArgumentBuilder<CommandContext> builder = command(literal)
                .requires(destination.getRequirement())
                .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
                .executes(destination.getCommand());
        for (final CommandNode<CommandContext> child : destination.getChildren()) {
            builder.then(child);
        }
        return builder;
    }

    public Void usageErrorHandler(final CommandContext context) {
        context.getEmbedBuilder()
                .title("Invalid command usage")
                .addField("Usage", commandUsage().serialize(context.getCommandSource()), false)
                .color(Color.RUBY);
        return null;
    }
}
