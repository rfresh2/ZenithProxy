package com.zenith.command.brigadier;

import com.google.common.base.CharMatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zenith.command.api.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.CommandProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.TimeProperties;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeArgument implements SerializableArgumentType<Integer> {
    private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType(new LiteralMessage("Invalid time unit"));
    private static final Dynamic2CommandExceptionType ERROR_TICK_COUNT_TOO_LOW = new Dynamic2CommandExceptionType(
        (tickCount, minTickCount) -> new LiteralMessage("tick count too low. min: " + minTickCount + ", provided: " + tickCount)
    );
    private static final Dynamic2CommandExceptionType ERROR_TICK_COUNT_TOO_HIGH = new Dynamic2CommandExceptionType(
        (tickCount, minTickCount) -> new LiteralMessage("tick count too high. max: " + minTickCount + ", provided: " + tickCount)
    );
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();
    static {
        UNITS.put("d", (int) (TimeUnit.DAYS.toMillis(1) / 50));
        UNITS.put("h", (int) (TimeUnit.HOURS.toMillis(1) / 50));
        UNITS.put("m", (int) (TimeUnit.MINUTES.toMillis(1) / 50));
        UNITS.put("s", (int) (TimeUnit.SECONDS.toMillis(1) / 50));
        UNITS.put("t", 1);
        UNITS.put("", 1);
    }
    final int minimum;
    final int maximum;

    public static TimeArgument time() {
        return new TimeArgument(0, Integer.MAX_VALUE);
    }

    public static TimeArgument time(int minimum) {
        return new TimeArgument(minimum, Integer.MAX_VALUE);
    }

    public static TimeArgument time(int minimum, int maximum) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("Maximum must be greater than or equal to minimum");
        }
        return new TimeArgument(minimum, maximum);
    }

    /**
     * returns ticks as int
     */
    public static Integer getTime(final com.mojang.brigadier.context.CommandContext<CommandContext> context, String name) {
        return context.getArgument(name, Integer.class);
    }

    public Integer parse(StringReader reader) throws CommandSyntaxException {
        float timeVal = reader.readFloat();
        String unitsStr = reader.readUnquotedString();
        int units = UNITS.getOrDefault(unitsStr, 0);
        if (units == 0) {
            throw ERROR_INVALID_UNIT.createWithContext(reader);
        } else {
            int time = Math.round(timeVal * units);
            if (time < this.minimum) {
                throw ERROR_TICK_COUNT_TOO_LOW.createWithContext(reader, time, this.minimum);
            } else if (time > this.maximum) {
                throw ERROR_TICK_COUNT_TOO_HIGH.createWithContext(reader, time, this.maximum);
            } else {
                return time;
            }
        }
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(com.mojang.brigadier.context.CommandContext context, SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getRemaining());
        try {
            stringReader.readFloat();
        } catch (CommandSyntaxException var5) {
            return builder.buildFuture();
        }
        return suggest(UNITS.keySet(), builder.createOffset(builder.getStart() + stringReader.getCursor()));
    }

    private static CharMatcher MATCH_SPLITTER = CharMatcher.anyOf("._/");

    private static boolean matchesSubStr(String input, String substring) {
        int i = 0;
        while (!substring.startsWith(input, i)) {
            int index = MATCH_SPLITTER.indexIn(substring, i);
            if (index < 0) {
                return false;
            }
            i = index + 1;
        }
        return true;
    }

    private static CompletableFuture<Suggestions> suggest(Iterable<String> strings, SuggestionsBuilder builder) {
        var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String s : strings) {
            if (matchesSubStr(remaining, s.toLowerCase(Locale.ROOT))) {
                builder.suggest(s);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public @NonNull CommandParser commandParser() {
        return CommandParser.TIME;
    }

    @Override
    public @Nullable CommandProperties commandProperties() {
        return new TimeProperties(minimum);
    }
}
