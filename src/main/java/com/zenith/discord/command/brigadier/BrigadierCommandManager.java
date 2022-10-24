package com.zenith.discord.command.brigadier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zenith.discord.command.brigadier.impl.AutoReconnectBrigadierCommand;
import com.zenith.discord.command.brigadier.impl.AutoUpdateBrigadierCommand;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.util.MultipartRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.DISCORD_LOG;
import static com.zenith.util.Constants.saveConfig;

public class BrigadierCommandManager {
    private final CommandDispatcher<CommandContext> dispatcher;

    public BrigadierCommandManager() {
        this.dispatcher = new CommandDispatcher<>();

        new AutoUpdateBrigadierCommand().register(dispatcher);
        new AutoReconnectBrigadierCommand().register(dispatcher);
    }

    public MultipartRequest<MessageCreateRequest> execute(final String message, final MessageCreateEvent messageCreateEvent) {
        final CommandContext context = new CommandContext(EmbedCreateSpec.builder(), messageCreateEvent);
        final ParseResults<CommandContext> parse = this.dispatcher.parse(downcaseFirstWord(message), context);
        try {
            executeWithErrorHandler(context, parse);
        } catch (final CommandSyntaxException e) {
            // fall through
            // errors handled by delegate
            // and if this not a matching root command we want to fallback to original commands
        }
        if (!context.getEmbedBuilder().build().isTitlePresent()) {
            DISCORD_LOG.debug("Failed executing brigadier command: {}", message);
            return null;
        } else {
            saveConfig();
            return MessageCreateSpec.builder()
                    .addEmbed(context.getEmbedBuilder()
                            .build())
                    .build().asRequest();
        }
    }

    private String downcaseFirstWord(final String sentence) {
        List<String> words = Arrays.asList(sentence.split(" "));
        if (words.size() > 1) {
            return words.get(0).toLowerCase() + words.stream().skip(1).collect(Collectors.joining(" ", " ", ""));
        } else {
            return sentence.toLowerCase();
        }
    }

    private int executeWithErrorHandler(final CommandContext context, final ParseResults<CommandContext> parse) throws CommandSyntaxException {
        final Optional<Function<CommandContext, Void>> errorHandler = parse.getContext().getNodes().stream().findFirst()
                .map(ParsedCommandNode::getNode)
                .filter(node -> node instanceof CaseInsensitiveLiteralCommandNode)
                .flatMap(commandNode -> ((CaseInsensitiveLiteralCommandNode<CommandContext>) commandNode).getErrorHandler());
        if (parse.getReader().canRead()) {
            errorHandler.ifPresent(handler -> handler.apply(context));
            return -1;
        }
        errorHandler.ifPresent(handler -> dispatcher.setConsumer((commandContext, success, result) -> {
            if (!success) handler.apply(context);
        }));

        return dispatcher.execute(parse);
    }
}
