package com.zenith.discord.command.brigadier;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CONFIG;

@Data
public class CommandUsage {
    private final String name;
    private final String description;
    private final List<String> usageLines;

    public String serialize() {
        return this.description
                + usageLines.stream()
                .map(line -> "\n  " + CONFIG.discord.prefix + name + " " + line)
                .collect(Collectors.joining());
    }
}
