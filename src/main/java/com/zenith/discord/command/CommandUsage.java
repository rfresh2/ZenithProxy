package com.zenith.discord.command;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CONFIG;

@Getter
@Setter
public class CommandUsage {
    private final String name;
    private final String description;
    private final List<String> usageLines;
    private final List<String> aliases;

    private CommandUsage(final String name, final String description, final List<String> usageLines, final List<String> aliases) {
        this.name = name;
        this.description = description;
        this.usageLines = usageLines;
        this.aliases = aliases;
    }

    public static CommandUsage simple(final String name, final String description) {
        return new CommandUsage(name, description, Collections.emptyList(), Collections.emptyList());
    }

    public static CommandUsage simpleAliases(final String name, final String description, final List<String> aliases) {
        return new CommandUsage(name, description, Collections.emptyList(), aliases);
    }

    public static CommandUsage args(final String name, final String description, final List<String> usageLines) {
        return new CommandUsage(name, description, usageLines, Collections.emptyList());
    }

    public static CommandUsage full(final String name, final String description, final List<String> usageLines, final List<String> aliases) {
        return new CommandUsage(name, description, usageLines, aliases);
    }

    public String serialize() {
        return this.description
                + usageLines.stream()
                .map(line -> "\n  " + CONFIG.discord.prefix + name + " " + line)
                .collect(Collectors.joining());
    }

    public String shortSerialize() {
        String result = CONFIG.discord.prefix + this.name;
        if (!aliases.isEmpty()) {
            result += aliases.stream()
                    .collect(Collectors.joining(" / .", " / .", ""));
        }
        return result;
    }
}
