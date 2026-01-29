package com.zenith.util;


import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.util.Set;
import java.util.regex.Pattern;

import static com.zenith.Globals.CONFIG;

public class ChatUtil {

    public static String sanitizeChatMessage(final String input) {
        StringBuilder stringbuilder = new StringBuilder();
        for (char c0 : input.toCharArray()) {
            if (isAllowedChatCharacter(c0)) {
                stringbuilder.append(c0);
            }
        }
        return constrainChatMessageSize(stringbuilder.toString(), true);
    }

    public static boolean isAllowedChatCharacter(char c0) {
        return c0 != 167 && c0 >= 32 && c0 != 127;
    }

    /**
     * @param end if true, characters from the end of the message will be removed (if needed).
     *            If false, characters from the start will be removed.
     */
    public static String constrainChatMessageSize(String message, boolean end) {
        if (message.length() <= 256) return message;
        if (end) {
            return message.substring(0, 256);
        } else {
            return message.substring(message.length() - 256);
        }
    }

    static final Pattern validUsernamePattern = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    public static boolean isValidPlayerName(String playerName) {
        return validUsernamePattern.matcher(playerName).matches();
    }

    private static final Set<String> knownWhisperCommands = Set.of(
        "w", "whisper", "msg", "minecraft:msg", "tell", "r"
    );
    @Deprecated
    public static boolean isWhisperCommand(String command) {
        if (CONFIG.client.extra.whisperCommand.equals(command)) return true;
        return knownWhisperCommands.contains(command);
    }

    public static ServerboundChatPacket getWhisperChatPacket(String playerName, String message) {
        return new ServerboundChatPacket(sanitizeChatMessage("/" + CONFIG.client.extra.whisperCommand + " " + playerName + " " + message));
    }
}
