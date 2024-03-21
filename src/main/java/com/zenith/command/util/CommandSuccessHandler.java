package com.zenith.command.util;

import com.zenith.command.brigadier.CommandContext;

@FunctionalInterface
public interface CommandSuccessHandler {
    void handle(CommandContext context);
}
