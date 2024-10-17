package com.zenith.terminal.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.CoreConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

public class MCTextFormatEncoder extends PatternLayoutEncoder {
    @Override
    public void start() {
        // insert our converter for minecraft text components, also should work for normal log messages
        Map<String, String> patternRuleRegistry = (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (isNull(patternRuleRegistry)) {
            patternRuleRegistry = new ConcurrentHashMap<>();
        }
        patternRuleRegistry.put("minecraftText", MCTextFormatConverter.class.getName());
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, patternRuleRegistry);
        super.start();
    }
}
