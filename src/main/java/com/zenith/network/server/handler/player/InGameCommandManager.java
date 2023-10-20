package com.zenith.network.server.handler.player;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandOutputHelper;
import com.zenith.command.CommandSource;
import com.zenith.network.server.ServerConnection;
import de.themoep.minedown.adventure.MineDown;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import static com.zenith.Shared.*;
import static java.util.Arrays.asList;

public class InGameCommandManager {

    // this is specific to CONTROLLING account commands - not spectator player commands!
    public void handleInGameCommand(final String command, final @NonNull ServerConnection session) {
        TERMINAL_LOG.info(session.getProfileCache().getProfile().getName() + " executed in-game command: " + command);
        switch (command) {
            case "help" -> {
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&9&lIn Game commands"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&2Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
                session.send(new ClientboundSystemChatPacket(Component.text(""), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cm &7- &8Sends a message to spectators"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&csync &7- &8Syncs current player inventory with server"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cchunksync &7- &8Syncs server chunks to the current player"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&ccleareffects &7- &8Clears all effects from the current player"), false));
                executeInGameCommand("help", session);
            }
            case "m" -> Proxy.getInstance().getActiveConnections().forEach(connection -> {
                connection.sendAsync(new ClientboundSystemChatPacket(MineDown.parse("&c" + session.getProfileCache().getProfile().getName() + " > " + command.substring(1).trim() + "&r"), false));
            });
            case "sync" -> {
                PlayerCache.sync();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSync inventory complete"), false));
            }
            case "chunksync" -> {
                ChunkCache.sync();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSync chunks complete"), false));
            }
            case "cleareffects" -> {
                CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().clear();
                asList(Effect.values()).forEach(effect -> {
                    session.send(new ClientboundRemoveMobEffectPacket(CACHE.getPlayerCache().getEntityId(), effect));
                });
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&9Cleared effects&r"), false));
            }
            default -> executeInGameCommand(command, session);
        }
    }

    private void executeInGameCommand(final String command, final ServerConnection session) {
        final CommandContext commandContext = CommandContext.create(command, CommandSource.IN_GAME_PLAYER);
        COMMAND_MANAGER.execute(commandContext);
        final EmbedCreateSpec embed = commandContext.getEmbedBuilder().build();
        CommandOutputHelper.logEmbedOutputToInGame(embed, session);
        CommandOutputHelper.logMultiLineOutputToInGame(commandContext, session);
        if (!embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty())
            session.send(new ClientboundSystemChatPacket(MineDown.parse(
                "&7[&9ZenithProxy&7]&r "
                    + "&cUnknown command! Command Prefix: \"" + CONFIG.inGameCommands.prefix + "\""), false));
        if (CONFIG.inGameCommands.logToDiscord && DISCORD_BOT.isRunning()) {
            // will also log to terminal
            CommandOutputHelper.logInputToDiscord(command);
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext);
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext);
        }
    }
}
