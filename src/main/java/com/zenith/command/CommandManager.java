package com.zenith.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandSources;
import com.zenith.command.brigadier.BrigadierToMCProtocolLibConverter;
import com.zenith.command.brigadier.CaseInsensitiveLiteralCommandNode;
import com.zenith.command.impl.*;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zenith.Globals.TERMINAL_LOG;
import static com.zenith.Globals.saveConfigAsync;
import static java.util.Arrays.asList;

@Getter
public class CommandManager {
    private final List<Command> commandsList = Lists.newArrayList(
        new ActionLimiterCommand(),
        new ActiveHoursCommand(),
        new AntiAFKCommand(),
        new AntiKickCommand(),
        new AntiLeakCommand(),
        new AuthCommand(),
        new AutoArmorCommand(),
        new AutoDisconnectCommand(),
        new AutoEatCommand(),
        new AutoFishCommand(),
        new AutoMendCommand(),
        new AutoOmenCommand(),
        new AutoReconnectCommand(),
        new AutoReplyCommand(),
        new AutoRespawnCommand(),
        new AutoTotemCommand(),
        new AutoUpdateCommand(),
        new ChatHistoryCommand(),
        new ChatRelayCommand(),
        new ChatSchemaCommand(),
        new ClickCommand(),
        new ClientConnectionCommand(),
        new CommandConfigCommand(),
        new ConnectCommand(),
        new ConnectionTestCommand(),
        new CoordinateObfuscationCommand(),
        new DatabaseCommand(),
        new DebugCommand(),
        new DeprecationWarningCommand(),
        new DisconnectCommand(),
        new DiscordManageCommand(),
        new DiscordNotificationsCommand(),
        new DisplayCoordsCommand(),
        new ExtraChatCommand(),
        new FriendCommand(),
        new HelpCommand(),
        new IgnoreCommand(),
        new InventoryCommand(),
        new JvmArgsCommand(),
        new KickCommand(),
        new KillAuraCommand(),
        new LicenseCommand(),
        new MapCommand(),
        new PathfinderCommand(),
        new PearlLoader(),
        new PlaytimeCommand(),
        new PluginsCommand(),
        new PrioCommand(),
        new QueueStatusCommand(),
        new QueueWarningCommand(),
        new RateLimiterCommand(),
        new RaycastCommand(),
        new ReconnectCommand(),
        new ReleaseChannelCommand(),
        new ReplayCommand(),
        new RequeueCommand(),
        new RespawnCommand(),
        new RotateCommand(),
        new SeenCommand(),
        new SendMessageCommand(),
        new ServerCommand(),
        new ServerConnectionCommand(),
        new ServerSwitcherCommand(),
        new SessionTimeLimitCommand(),
        new SkinCommand(),
        new SpammerCommand(),
        new SpectatorCommand(),
        new SpectatorEntityCommand(),
        new SpectatorEntityToggleCommand(),
        new SpectatorPlayerCamCommand(),
        new SpectatorSwapCommand(),
        new SpookCommand(),
        new StalkCommand(),
        new StatsCommand(),
        new StatusCommand(),
        new TablistCommand(),
        new SpawnPatrolCommand(),
        new ThemeCommand(),
        new TransferCommand(),
        new UpdateCommand(),
        new UnsupportedCommand(),
        new ViaVersionCommand(),
        new VisualRangeCommand(),
        new WhitelistCommand()
    );
    private final CommandDispatcher<CommandContext> dispatcher;
    @Getter private @NonNull CommandNode[] MCProtocolLibCommandNodes;

    public CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        registerCommands();
        syncCommandNodes();
    }

    public void registerCommands() {
       commandsList.forEach(this::registerCommand);
    }

    public void registerPluginCommand(Command command) {
        registerCommand(command);
        commandsList.add(command);
        syncCommandNodes();
    }

    public List<Command> getCommands() {
        return commandsList;
    }

    public List<Command> getCommands(final CommandCategory category) {
        return commandsList.stream()
            .filter(command -> category == CommandCategory.ALL || command.commandUsage().getCategory() == category)
            .toList();
    }

    void registerCommand(final Command command) {
        final LiteralCommandNode<CommandContext> node = dispatcher.register(command.register());
        command.commandUsage().getAliases().forEach(alias -> dispatcher.register(command.redirect(alias, node)));
    }

    void syncCommandNodes() {
        this.MCProtocolLibCommandNodes = BrigadierToMCProtocolLibConverter.convertNodesToMCProtocolLibNodes(this.dispatcher);
    }

    public void execute(final CommandContext context, final ParseResults<CommandContext> parseResults) {
        try {
            execute0(context, parseResults);
        } catch (final CommandSyntaxException e) {
            // fall through
            // errors handled by delegate
            // and if this not a matching root command we want to fallback to original commands
        }
        saveConfigAsync();
    }

    public void execute(final CommandContext context) {
        final ParseResults<CommandContext> parse = parse(context);
        execute(context, parse);
    }

    public ParseResults<CommandContext> parse(final CommandContext context) {
        return this.dispatcher.parse(downcaseFirstWord(context.getInput()), context);
    }

    public boolean hasCommandNode(final ParseResults<CommandContext> parse) {
        return parse.getContext().getNodes().stream().anyMatch(node -> node.getNode() instanceof CaseInsensitiveLiteralCommandNode);
    }

    private String downcaseFirstWord(final String sentence) {
        List<String> words = asList(sentence.split(" "));
        if (words.size() > 1) {
            return words.getFirst().toLowerCase() + words.stream().skip(1).collect(Collectors.joining(" ", " ", ""));
        } else {
            return sentence.toLowerCase();
        }
    }

    private void execute0(final CommandContext context, final ParseResults<CommandContext> parse) throws CommandSyntaxException {
        var commandNodeOptional = parse.getContext()
            .getNodes()
            .stream()
            .findFirst()
            .map(ParsedCommandNode::getNode)
            .filter(node -> node instanceof CaseInsensitiveLiteralCommandNode)
            .map(node -> ((CaseInsensitiveLiteralCommandNode<CommandContext>) node));
        if (commandNodeOptional.isEmpty()) return;
        var commandNode = commandNodeOptional.get();
        var errorHandler = commandNode.getErrorHandler();
        var successHandler = commandNode.getSuccessHandler();
        var executionErrorHandler = commandNode.getExecutionErrorHandler();

        if (!parse.getExceptions().isEmpty() || parse.getReader().canRead()) {
            errorHandler.handle(parse.getExceptions(), context);
            return;
        }
        dispatcher.setConsumer((commandContext, success, result) -> {
            if (success) {
                if (result == Command.OK)
                    successHandler.handle(context);
                else
                    executionErrorHandler.handle(context);
            }
            else errorHandler.handle(parse.getExceptions(), context);
        });
        dispatcher.execute(parse);
    }

    public List<String> getCommandCompletions(final String input) {
        final ParseResults<CommandContext> parse = this.dispatcher.parse(downcaseFirstWord(input), CommandContext.create(input, CommandSources.TERMINAL));
        try {
            var suggestions = this.dispatcher.getCompletionSuggestions(parse).get(2L, TimeUnit.SECONDS);
            return suggestions.getList().stream()
                .map(Suggestion::getText)
                .toList();
        } catch (final Exception e) {
            TERMINAL_LOG.warn("Failed to get command completions for input: {}", input);
            return List.of();
        }
    }
}
