package com.zenith.network.server.handler.player;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.command.util.CommandOutputHelper;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;

import java.util.regex.Pattern;

import static com.zenith.Shared.*;

public class InGameCommandManager {
    private Pattern commandPattern;
    private String commandPatternPrefix = "";

    // this is specific to CONTROLLING account commands - not spectator player commands!
    // true = command was handled
    // false = command was not handled
    public boolean handleInGameCommand(final String message, final @NonNull ServerConnection session, final boolean printUnhandled) {
        TERMINAL_LOG.info(session.getProfileCache().getProfile().getName() + " executed in-game command: " + message);
        final String command = message.split(" ")[0]; // first word is the command
        if (command.equals("help") && CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minedown("&9&lIn Game commands"),
                false));
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minedown("&2Prefix : \"" + CONFIG.inGameCommands.prefix + "\""),
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

    private boolean executeInGameCommand(final String command, final ServerConnection session, final boolean printUnhandled) {
        final CommandContext commandContext = CommandContext.create(command, CommandSource.IN_GAME_PLAYER);
        // todo: execute commands async wtf!
        //  all we need to do is make sure a corresponding root command node exists and return the boolean value there
        COMMAND.execute(commandContext);
        var embed = commandContext.getEmbed();
        CommandOutputHelper.logEmbedOutputToInGame(embed, session);
        CommandOutputHelper.logMultiLineOutputToInGame(commandContext, session);
        if (!commandContext.isNoOutput() && !embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty()) {
            if (printUnhandled) {
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown(
                    "&7[&9ZenithProxy&7]&r &cUnknown command!"), false));
            }
            return false;
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
        return true;
    }
}
