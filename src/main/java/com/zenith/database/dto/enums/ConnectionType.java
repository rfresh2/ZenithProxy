package com.zenith.database.dto.enums;


import lombok.Getter;

@Getter
public enum ConnectionType {

    JOIN("JOIN"),

    LEAVE("LEAVE");

    private final String literal;

    ConnectionType(String literal) {
        this.literal = literal;
    }
}
