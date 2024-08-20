package com.zenith.command.brigadier;

import lombok.Getter;

@Getter
public enum CommandSource {
    TERMINAL("Terminal"),
    DISCORD("Discord"),
    IN_GAME_PLAYER("In-Game"),
    SPECTATOR("Spectator");
    private final String name;
    CommandSource(final String name) {
        this.name = name;
    }

}
