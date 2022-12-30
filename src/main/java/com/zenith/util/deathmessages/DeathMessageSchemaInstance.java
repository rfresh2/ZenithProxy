package com.zenith.util.deathmessages;

import lombok.Data;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;

@Data
public final class DeathMessageSchemaInstance {
    private final String schemaRaw;
    private final List<String> schema;

    public DeathMessageSchemaInstance(final String schemaRaw) {
        this.schemaRaw = schemaRaw;
        this.schema = Arrays.asList(schemaRaw.split(" "));
    }

    public Optional<DeathMessageParseResult> parse(final MCTextRoot mcTextRoot) {
        final MCTextRootIterator iterator = new MCTextRootIterator(mcTextRoot);
        String victim = null;
        String killer = null;
        String weapon = null;
        for (int i = 0; i < schema.size(); i++) {
            final String schemaWord = schema.get(i);
            final MCTextWord mcTextWord = iterator.next();
            if (isNull(mcTextWord)) break;
            if (schemaWord.startsWith("$v")) {
                if (!mcTextWord.isKeyword) {
                    return Optional.empty();
                } else {
                    victim = mcTextWord.word;
                }
            } else if (schemaWord.startsWith("$k")) {
                if (!mcTextWord.isKeyword) {
                    return Optional.empty();
                } else {
                    killer = mcTextWord.word;
                }
            } else if (schemaWord.startsWith("$w")) {
                if (!mcTextWord.isKeyword) {
                    return Optional.empty();
                } else {
                    weapon = mcTextWord.word;
                }
            } else {
                if (mcTextWord.isKeyword) {
                    return Optional.empty();
                } else {
                    if (schemaWord.equals("a(n)")) {
                        // special case for grammar
                        if (!(mcTextWord.word.equals("a") || mcTextWord.word.equals("an"))) {
                            return Optional.empty();
                        }
                    } else if (schemaWord.equals("(with)")) {
                        // special case for "with"/"using" alt shorthand
                        if (!(mcTextWord.word.equals("with") || mcTextWord.word.equals("using"))) {
                            return Optional.empty();
                        }
                    } else {
                        if (!mcTextWord.word.equals(schemaWord)) {
                            return Optional.empty();
                        }
                    }
                }
            }
        }
        if (isNull(victim)) return Optional.empty(); // we shouldn't ever reach this but just in case
        return Optional.of(new DeathMessageParseResult(victim, Optional.ofNullable(killer), Optional.ofNullable(weapon), this));
    }
}
