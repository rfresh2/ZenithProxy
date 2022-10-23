package com.zenith.discord.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.util.MultipartRequest;

import static com.zenith.util.Constants.DISCORD_LOG;

public class BrigadierCommandManager {
    private final CommandDispatcher<CommandContext> dispatcher;

    public BrigadierCommandManager() {
        this.dispatcher = new CommandDispatcher<>();

        new AutoUpdateBrigadierCommand().register(dispatcher);
    }

    public MultipartRequest<MessageCreateRequest> execute(final String message, final MessageCreateEvent messageCreateEvent) {
        try {
            final CommandContext context = new CommandContext(EmbedCreateSpec.builder(), messageCreateEvent);
            final ParseResults<CommandContext> parse = this.dispatcher.parse(message, context);
            dispatcher.execute(parse);
            return MessageCreateSpec.builder()
                    .addEmbed(context.getEmbedBuilder()
                            .build())
                    .build().asRequest();
        } catch (final RuntimeException | CommandSyntaxException e) {
            DISCORD_LOG.error("Failed executing command: {}", message, e);
            return null;
        }
    }
}
