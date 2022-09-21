package com.zenith.util.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.NonNull;
import net.daporkchop.lib.logging.console.ansi.VGAColor;
import net.daporkchop.lib.logging.format.FormatParser;
import net.daporkchop.lib.logging.format.TextStyle;
import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import static net.daporkchop.lib.logging.console.ansi.ANSI.ESC;

/**
 * All the ANSI formatting stuff is taken from PorkLib:
 * https://github.com/PorkStudios/PorkLib/blob/development/logging/src/main/java/net/daporkchop/lib/logging/console/ansi/
 */
public class MCTextFormatANSIConverter extends MessageConverter {
    private final FormatParser formatParser = AutoMCFormatParser.DEFAULT;

    protected static String getUpdateTextFormatCommand(VGAColor textColor, VGAColor backgroundColor, int style) {
        return String.format(
                "%c[0;%d;%d%sm",
                ESC,
                textColor.getFg(),
                backgroundColor.getBg(),
                getStyleStuff(style)
        );
    }

    protected static CharSequence getStyleStuff(int style) {
        if (TextStyle.isDefault(style)) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder(); //TODO: pool these
            if (TextStyle.isBold(style)) {
                builder.append(";1");
            }
            if (TextStyle.isItalic(style)) {
                builder.append(";3");
            }
            if (TextStyle.isUnderline(style)) {
                builder.append(";4");
            }
            if (TextStyle.isStrikethrough(style)) {
                builder.append(";9");
            }
            if (TextStyle.isOverline(style)) {
                builder.append(";53");
            }
            if (TextStyle.isBlinking(style)) {
                builder.append(";5");
            }
            return builder;
        }
    }

    @Override
    public String convert(ILoggingEvent event) {
        TextComponent textComponent = formatParser.parse(event.getFormattedMessage());
        StringBuilder builder = new StringBuilder();
        this.doBuild(builder, textComponent);
        return builder.append(ESC).append("[0m").toString();
    }

    protected void doBuild(@NonNull StringBuilder builder, @NonNull TextComponent component) {
        String text = component.getText();
        if (text != null && !text.isEmpty()) {
            builder.append(MCTextFormatANSIConverter.getUpdateTextFormatCommand(
                    VGAColor.closestTo(component.getColor()),
                    VGAColor.closestTo(component.getBackgroundColor()),
                    component.getStyle()
            )).append(text);
        }
        for (TextComponent child : component.getChildren()) {
            this.doBuild(builder, child);
        }
    }
}
