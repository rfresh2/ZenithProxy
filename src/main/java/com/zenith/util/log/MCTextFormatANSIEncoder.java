package com.zenith.util.log;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.CoreConstants;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

import static java.util.Objects.isNull;

public class MCTextFormatANSIEncoder extends PatternLayoutEncoder {

    @Override
    public void start() {
        // insert our converter for minecraft text components, also should work for normal log messages
        Map patternRuleRegistry = (Map) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (isNull(patternRuleRegistry)) {
            patternRuleRegistry = new Object2ObjectOpenHashMap<>();
        }
        patternRuleRegistry.put("minecraftText", MCTextFormatANSIConverter.class.getName());
        context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, patternRuleRegistry);
        super.start();
    }

}
