package com.zenith.terminal.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LoggerNameUtil;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogSourceFilter extends Filter<ILoggingEvent> {
    private static final Map<String, Level> SOURCE_MAP = new HashMap<>();
    static {
        SOURCE_MAP.put("org.jline", Level.WARN);
        SOURCE_MAP.put("net.dv8tion.jda", Level.INFO);
        SOURCE_MAP.put("org.geysermc", Level.DEBUG);
        SOURCE_MAP.put("ViaVersion", Level.INFO);
        SOURCE_MAP.put("ViaBackwards", Level.INFO);
        SOURCE_MAP.put("Proxy", Level.TRACE);
        SOURCE_MAP.put("Auth", Level.TRACE);
        SOURCE_MAP.put("Cache", Level.TRACE);
        SOURCE_MAP.put("Client", Level.TRACE);
        SOURCE_MAP.put("Chat", Level.TRACE);
        SOURCE_MAP.put("Module", Level.TRACE);
        SOURCE_MAP.put("Server", Level.TRACE);
        SOURCE_MAP.put("Discord", Level.TRACE);
        SOURCE_MAP.put("Database", Level.TRACE);
        SOURCE_MAP.put("Terminal", Level.TRACE);
        SOURCE_MAP.put("Plugin", Level.TRACE);
        SOURCE_MAP.put("Pathfinder", Level.TRACE);
    }
    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    public static void registerLogger(final String loggerName, final Level level) {
        if (SOURCE_MAP.containsKey(loggerName)) return;
        SOURCE_MAP.put(loggerName, level);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(loggerName).setLevel(level);
    }

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        if (INIT.compareAndSet(false, true)) {
            SOURCE_MAP.forEach((s, level) -> {
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                lc.getLogger(s).setLevel(level);
            });
        }
        var loggerName = event.getLoggerName();
        if (SOURCE_MAP.containsKey(loggerName)) {
            return FilterReply.NEUTRAL;
        }
        int fromIndex = 0;
        String src = "";
        while (true) {
            int index = LoggerNameUtil.getSeparatorIndexOf(loggerName, fromIndex);
            if (index == -1) {
                return FilterReply.DENY;
            }
            String s = loggerName.substring(fromIndex, index);
            if (src.isEmpty()) {
                src = s;
            } else {
                src += "." + s;
            }
            if (SOURCE_MAP.containsKey(src)) {
                return FilterReply.NEUTRAL;
            }
            fromIndex = index + 1;
        }
    }
}
