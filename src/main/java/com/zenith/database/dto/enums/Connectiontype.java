package com.zenith.database.dto.enums;


import lombok.Getter;

@Getter
public enum Connectiontype {

    JOIN("JOIN"),

    LEAVE("LEAVE");

    private final String literal;

    Connectiontype(String literal) {
        this.literal = literal;
    }
}
