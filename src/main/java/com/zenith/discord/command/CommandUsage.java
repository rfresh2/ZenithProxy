package com.zenith.discord.command;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CONFIG;

@Getter
@Setter
public class CommandUsage {
    private final String name;
    private final String description;
    private final List<String> usageLines;

    private CommandUsage(final String name, final String description, final List<String> usageLines) {
        this.name = name;
        this.description = description;
        this.usageLines = usageLines;
    }

    public static CommandUsage of(final String name, final String description, final List<String> usageLines) {
        return new CommandUsage(name, description, usageLines);
    }

    public String serialize() {
        return this.description
                + usageLines.stream()
                .map(line -> "\n  " + CONFIG.discord.prefix + name + " " + line)
                .collect(Collectors.joining());
    }

    public String shortSerialize() {
        return CONFIG.discord.prefix + this.name;
    }
}
