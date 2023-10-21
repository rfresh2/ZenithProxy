package com.zenith.command;

import lombok.Getter;

@Getter
public enum CommandSource {
    TERMINAL("Terminal"),
    DISCORD("Discord"),
    IN_GAME_PLAYER("In-Game");
    private final String name;
    CommandSource(final String name) {
        this.name = name;
    }

}
