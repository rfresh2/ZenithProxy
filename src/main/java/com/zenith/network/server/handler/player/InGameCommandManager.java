package com.zenith.network.server.handler.player;

import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.command.util.CommandOutputHelper;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.regex.Pattern;

import static com.zenith.Shared.*;

public class InGameCommandManager {
    private Pattern commandPattern;
    private String commandPatternPrefix = "";

    // this is specific to CONTROLLING account commands - not spectator player commands!
    // true = command was handled
    // false = command was not handled
    public boolean handleInGameCommand(final String message, final @NonNull ServerSession session, final boolean printUnhandled) {
        TERMINAL_LOG.info("{} executed in-game command: {}", session.getProfileCache().getProfile().getName(), message);
        final String command = message.split(" ")[0]; // first word is the command
        if (command.equals("help") && CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minedown("&9&lIn Game commands"),
                false));
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minedown("&aPrefix : \"" + CONFIG.inGameCommands.prefix + "\""),
                false));
        }
        return executeInGameCommand(message, session, printUnhandled);
    }

    public Pattern getCommandPattern() {
        if (!this.commandPatternPrefix.equals(CONFIG.inGameCommands.prefix))
            this.commandPattern = buildCommandPattern();
        return this.commandPattern;
    }

    private Pattern buildCommandPattern() {
        this.commandPatternPrefix = CONFIG.inGameCommands.prefix;
        return Pattern.compile("[" + CONFIG.inGameCommands.prefix + "]\\w+");
    }

    private boolean executeInGameCommand(final String command, final ServerSession session, final boolean printUnhandled) {
        final CommandContext commandContext = CommandContext.createInGamePlayerContext(command, session);
        var parse = COMMAND.parse(commandContext);
        if (!COMMAND.hasCommandNode(parse)) return false;
        EXECUTOR.execute(() -> {
            COMMAND.execute(commandContext, parse);
            var embed = commandContext.getEmbed();
            CommandOutputHelper.logEmbedOutputToInGame(embed, session);
            CommandOutputHelper.logMultiLineOutputToInGame(commandContext, session);
            if (!commandContext.isNoOutput() && !embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty()) {
                if (printUnhandled) {
                    session.sendAsyncAlert("&cUnknown command!");
                }
                return;
            }
            if (CONFIG.inGameCommands.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
                // will also log to terminal
                CommandOutputHelper.logInputToDiscord(command, CommandSource.IN_GAME_PLAYER);
                CommandOutputHelper.logEmbedOutputToDiscord(embed);
                CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
            } else {
                CommandOutputHelper.logEmbedOutputToTerminal(embed);
                CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
            }
        });
        return true;
    }

    public void handleInGameCommandSpectator(final String message, final @NonNull ServerSession session, final boolean printUnhandled) {
        TERMINAL_LOG.info("{} executed in-game spectator command: {}", session.getProfileCache().getProfile().getName(), message);
        final CommandContext commandContext = CommandContext.createSpectatorContext(message, session);
        var parse = COMMAND.parse(commandContext);
        if (COMMAND.hasCommandNode(parse)) {
            COMMAND.execute(commandContext, parse);
        }
        var embed = commandContext.getEmbed();
        CommandOutputHelper.logEmbedOutputToInGame(embed, session);
        CommandOutputHelper.logMultiLineOutputToInGame(commandContext, session);
        if (!commandContext.isNoOutput() && !embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty()) {
            if (printUnhandled) {
                session.sendAsyncAlert("&cUnknown command!");
            }
            return;
        }
        if (CONFIG.inGameCommands.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
            // will also log to terminal
            CommandOutputHelper.logInputToDiscord(message, CommandSource.SPECTATOR);
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
        }
    }
}
