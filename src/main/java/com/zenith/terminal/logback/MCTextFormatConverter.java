package com.zenith.terminal.logback;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.daporkchop.lib.logging.format.FormatParser;
import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

public class MCTextFormatConverter extends MessageConverter {
    private final FormatParser formatParser = AutoMCFormatParser.DEFAULT;

    @Override
    public String convert(ILoggingEvent event) {
        final String formattedMessage = event.getFormattedMessage();
        try {
            // if the message doesn't start with a curly brace it ain't json
            if (formattedMessage.startsWith("{") || formattedMessage.contains("§")) {
                TextComponent textComponent = formatParser.parse(formattedMessage);
                return textComponent.toRawString();
            }
        } catch (final Exception e) {
            // fall through
        }
        return formattedMessage;
    }
}
