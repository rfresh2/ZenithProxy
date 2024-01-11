package com.zenith.terminal.logback;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zenith.util.ComponentSerializer;

public class MCTextFormatANSIConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        var formattedMessage = event.getFormattedMessage();
        if (formattedMessage.startsWith("{") || formattedMessage.contains("§"))
            return ComponentSerializer.serializeAnsi(ComponentSerializer.deserialize(formattedMessage));
        else
            return formattedMessage;
    }
}
