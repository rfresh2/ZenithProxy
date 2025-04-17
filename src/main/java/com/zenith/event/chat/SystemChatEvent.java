package com.zenith.event.chat;

import net.kyori.adventure.text.Component;

public record SystemChatEvent(
    Component component,
    String message,
    boolean isUnresolvedPublicChat,
    boolean isUnresolvedWhisper,
    boolean isUnresolvedIncomingWhisper
) { }
