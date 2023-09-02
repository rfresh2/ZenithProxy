package com.zenith.terminal.logback;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;

public class MCTextFormatANSIConverter extends MessageConverter {
    private static final char ESC = 0x1B;
    private static final String finalizer = ESC + "[0m";

    @Override
    public String convert(ILoggingEvent event) {
        String formattedMessage = event.getFormattedMessage();
        if (formattedMessage.startsWith("{") || formattedMessage.contains("§")) {
            Component component = ComponentSerializer.translate(ComponentSerializer.deserialize(formattedMessage));
            return ANSIComponentSerializer.ansi().serialize(component) + finalizer;
        } else {
            return formattedMessage;
        }
    }
}
