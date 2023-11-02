package com.zenith.command;

import lombok.Getter;

@Getter
public enum CommandCategory {
    // We should keep the number of categories small
    CORE("Core"),
    STATUS("Status"),
    MANAGE("Manage"),
    MODULE("Module"),
    ALL("All"); // this shouldn't be assigned to any command, used as an info wildcard
    private final String name;

    CommandCategory(final String name) {
        this.name = name;
    }

}
