package com.zenith.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomString {

    private static final char[] CHARS;

    static {
        var symbol = new StringBuilder();
        for (char character = '0'; character <= '9'; character++) {
            symbol.append(character);
        }
        for (char character = 'a'; character <= 'z'; character++) {
            symbol.append(character);
        }
        for (char character = 'A'; character <= 'Z'; character++) {
            symbol.append(character);
        }
        CHARS = symbol.toString().toCharArray();
    }

    public static String generate(int len) {
        var result = new StringBuilder();
        for (int i = 0; i < len; i++) {
            var index = ThreadLocalRandom.current().nextInt(0, CHARS.length);
            var randomChar = CHARS[index];
            result.append(randomChar);
        }
        return result.toString();
    }

}
