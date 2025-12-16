package com.zenith.util;

import java.util.UUID;

public class BedrockUtil {
    private BedrockUtil() {}

    public static boolean isBedrock(String username) {
        return username.startsWith(".");
    }

    public static boolean isBedrock(UUID uuid) {
        if (uuid.getMostSignificantBits() != 0L) return false;
        if (uuid.getLeastSignificantBits() == 0L) return false;
        return Long.compareUnsigned(uuid.getLeastSignificantBits(), 0x0009000000000000L) > 0;
    }
}
