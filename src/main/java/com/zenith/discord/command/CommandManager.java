package com.zenith.discord.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.discord.command.impl.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.MultipartRequest;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.DISCORD_LOG;
import static com.zenith.util.Constants.saveConfig;
import static java.util.Arrays.asList;

@Getter
public class CommandManager {
    private final CommandDispatcher<CommandContext> dispatcher;
    private List<Command> commands;

    public CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        registerCommands();
    }

    private void registerCommands() {
        this.commands = asList(
                new ActiveHoursCommand(),
                new AntiAFKCommand(),
                new AutoDisconnectCommand(),
                new AutoReconnectCommand(),
                new AutoReplyCommand(),
                new AutoRespawnCommand(),
                new AutoUpdateCommand(),
                new ChatRelayCommand(),
                new ConnectCommand(),
                new DatabaseCommand(),
                new DisconnectCommand(),
                new DisplayCoordsCommand(),
                new HelpCommand(),
                new KickCommand(),
                new PrioCommand(),
                new ProxyClientConnectionCommand(),
                new QueueWarningCommand(),
                new ReconnectCommand(),
                new RespawnCommand(),
                new ServerCommand(),
                new StalkCommand(),
                new StatusCommand(),
                new SpectatorCommand(),
                new SpookCommand(),
                new TablistCommand(),
                new UpdateCommand(),
                new VisualRangeCommand(),
                new WhitelistCommand()
        );
        this.commands.forEach(this::registerCommand);
    }

    private void registerCommand(final Command command) {
        final LiteralCommandNode<CommandContext> node = dispatcher.register(command.register());
        command.aliases().forEach(alias -> dispatcher.register(command.redirect(alias, node)));
    }

    public MultipartRequest<MessageCreateRequest> execute(final String message, final MessageCreateEvent messageCreateEvent, final RestChannel restChannel) {
        final CommandContext context = new CommandContext(EmbedCreateSpec.builder(), messageCreateEvent, this, restChannel);
        final ParseResults<CommandContext> parse = this.dispatcher.parse(downcaseFirstWord(message), context);
        try {
            executeWithErrorHandler(context, parse);
        } catch (final CommandSyntaxException e) {
            // fall through
            // errors handled by delegate
            // and if this not a matching root command we want to fallback to original commands
        }
        if (!context.getEmbedBuilder().build().isTitlePresent()) {
            DISCORD_LOG.debug("Brigadier command returned no embed: {}", message);
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
        List<String> words = asList(sentence.split(" "));
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
