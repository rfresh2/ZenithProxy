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
        try {
            TextComponent textComponent = formatParser.parse(event.getFormattedMessage());
            return textComponent.toRawString();
        } catch (final Exception e) {
            return event.getFormattedMessage();
        }
    }
}
