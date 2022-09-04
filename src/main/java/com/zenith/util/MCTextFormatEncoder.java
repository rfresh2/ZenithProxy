package com.zenith.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;
import net.daporkchop.lib.logging.format.FormatParser;
import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

public class MCTextFormatEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    private final FormatParser formatParser = AutoMCFormatParser.DEFAULT;

    @Override
    public void start() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(getPattern());
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        TextComponent textComponent = formatParser.parse(event.getMessage());
        event.getMessage();
        return super.encode(event);
    }
}
