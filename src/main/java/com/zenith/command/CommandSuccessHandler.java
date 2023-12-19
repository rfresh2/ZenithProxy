package com.zenith.command;

@FunctionalInterface
public interface CommandSuccessHandler {
    void handle(CommandContext context);
}
