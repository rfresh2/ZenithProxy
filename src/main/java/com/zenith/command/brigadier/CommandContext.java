package com.zenith.command.brigadier;

import com.zenith.discord.Embed;
import com.zenith.network.server.ServerSession;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandContext {
    private final String input;
    private final CommandSource source;
    private final Embed embed;
    private final List<String> multiLineOutput;
    private @Nullable InGamePlayerInfo inGamePlayerInfo;
    // don't log sensitive input like passwords to discord
    private boolean sensitiveInput = false;
    private boolean noOutput = false;

    public CommandContext(String input, CommandSource source, Embed embed, List<String> multiLineOutput) {
        this.input = input;
        this.source = source;
        this.embed = embed;
        this.multiLineOutput = multiLineOutput;
    }

    public static CommandContext create(final String input, final CommandSource source) {
        return new CommandContext(input, source, new Embed(), new ArrayList<>(0));
    }

    public static CommandContext createInGamePlayerContext(String input, ServerSession session) {
        var context = create(input, CommandSource.IN_GAME_PLAYER);
        context.setInGamePlayerInfo(new InGamePlayerInfo(session));
        return context;
    }

    public static CommandContext createSpectatorContext(String input, ServerSession session) {
        var context = create(input, CommandSource.SPECTATOR);
        context.setInGamePlayerInfo(new InGamePlayerInfo(session));
        return context;
    }

    public record InGamePlayerInfo(ServerSession session) {}
}
