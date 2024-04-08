package com.zenith.terminal.logback;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;

public class MCTextFormatConverter extends MessageConverter {
    @Override
    public String convert(ILoggingEvent event) {
        final String formattedMessage = event.getFormattedMessage();
        try {
            // if the message doesn't start with a curly brace it ain't json
            if (formattedMessage.startsWith("{") || formattedMessage.contains("§")) {
                Component component = ComponentSerializer.deserialize(formattedMessage);
                return ComponentSerializer.serializePlainWithLinks(component);
            }
        } catch (final Exception e) {
            // fall through
        }
        return formattedMessage;
    }
}
