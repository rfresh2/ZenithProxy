package com.zenith.util.deathmessages;

import lombok.Data;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

@Data
public final class DeathMessageSchemaInstance {
    private final String schemaRaw;
    private final List<String> schema;
    static final List<String> mobTypes = asList(
            "zombie pigman",
            "zombie pigmen",
            "creeper",
            "wither",
            "zombie",
            "slime",
            "slime cube",
            "magma cube",
            "zombies",
            "skeletons",
            "skeleton",
            "spider",
            "skeletal warrior",
            "endermite",
            "enderman",
            "wolf",
            "witch",
            "zombie villager",
            "ghast",
            "husk",
            "ender dragon"
    );

    public DeathMessageSchemaInstance(final String schemaRaw) {
        this.schemaRaw = schemaRaw;
        this.schema = asList(schemaRaw.split(" "));
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
            } else if (schemaWord.contains("$m")) { // iterator doesn't split these out
                if (mcTextWord.isKeyword) {
                    return Optional.empty();
                } else {
                    if (mcTextWord.word.equals("a") || mcTextWord.word.equals("an")) {
                        i--; // skips any a or an prefix to a mob type
                        continue;
                    }
                    boolean found = false;
                    for (String mobType : mobTypes) {
                        // mcTextWord.word can have multiple spaces here - special parsing case
                        if (mcTextWord.word.startsWith(mobType)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return Optional.empty();
                    }
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
