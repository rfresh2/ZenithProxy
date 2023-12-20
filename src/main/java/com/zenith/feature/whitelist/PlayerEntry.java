package com.zenith.feature.whitelist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(exclude = {"lastRefreshed"})
@AllArgsConstructor
public class PlayerEntry {
    private String username;
    private UUID uuid;
    // epoch second
    private Long lastRefreshed;
    public PlayerEntry() {} // for gson deserialization
    public String getNameMCLink() {
        return "https://namemc.com/profile/" + uuid.toString();
    }
}
