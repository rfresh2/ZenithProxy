package com.zenith.feature.deathmessages;

import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.zenith.Shared.DEFAULT_LOG;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

@Data
public final class DeathMessageSchemaInstance {
    private final String schemaRaw;
    private final List<String> schema;
    private static final Pattern userNameValidPattern = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private final List<String> mobs;

    public DeathMessageSchemaInstance(final String schemaRaw, final List<String> mobs) {
        this.schemaRaw = schemaRaw;
        this.schema = spaceSplit(schemaRaw);
        this.mobs = mobs;
    }

    public static List<String> spaceSplit(final String in) {
        return asList(in.split(" "));
    }

    public Optional<DeathMessageParseResult> parse(final List<String> input, final List<String> playerNames) {
        final WordIterator iterator = new WordIterator(input);
        String victim = null;
        Killer killer = null;
        String weapon = null;

        OUTER_LOOP:
        for (int i = 0; i < schema.size(); i++) {
            final String schemaWord = schema.get(i);
            final String mcTextWord = iterator.next();
            if (isNull(mcTextWord)) return Optional.empty();
            if (schemaWord.startsWith("$v")) {
                String textWord = mcTextWord;
                if (schemaWord.endsWith("'s")) { // handle special case with apostrophe
                    textWord = mcTextWord.replace("'s", "");
                }
                if (!userNameValidPattern.matcher(textWord).matches()) {
                    return Optional.empty();
                } else {
                    if (!playerNames.contains(textWord)) return Optional.empty();
                    victim = textWord;
                }
            } else if (schemaWord.startsWith("$k")) {
                if (!userNameValidPattern.matcher(mcTextWord).matches()) {
                    return Optional.empty();
                } else {
                    if (!playerNames.contains(mcTextWord)) return Optional.empty();
                    killer = new Killer(mcTextWord, KillerType.PLAYER);
                }
            } else if (schemaWord.startsWith("$w")) {
                // we want to match just about any character and multiple words here
                // if there are additional schema words after $w we want to ensure we match those too though
                if (i + 1 < schema.size()) {
                    // peek next word and match any words until then
                    final String schemaPeek = schema.get(i + 1);
                    String weaponWip = mcTextWord;
                    while (iterator.hasNext()) {
                        final String next = iterator.next();
                        if (next.equals(schemaPeek) && iterator.peekToEnd().stream().noneMatch(w -> w.equals(schemaPeek))) {
                            i++;
                            weapon = weaponWip;
                            continue OUTER_LOOP;
                        } else {
                            weaponWip += " " + next;
                        }
                    }
                    return Optional.empty(); // we didn't match on subsequent words
                } else {
                    String weaponWip = mcTextWord;
                    while (iterator.hasNext()) {
                        final String next = iterator.next();
                        weaponWip += " " + next;
                    }
                    weapon = weaponWip;
                }
            } else if (schemaWord.startsWith("$m")) { // iterator doesn't split these out
                if (mcTextWord.equals("a") || mcTextWord.equals("an")) {
                    i--; // skips any a or an prefix to a mob type
                    continue;
                }

                // this entire loop is horrifying
                List<String> strings = this.mobs;
                for (int j = 0; j < strings.size(); j++) {
                    final String mobType = strings.get(j); // only works if mobs is sorted by longest first :/
                    final List<String> mobSplit = spaceSplit(mobType);
                    if (mobSplit.get(0).equals(mcTextWord.replace(".", ""))) {
                        if (mobSplit.size() > 1) {
                            if (iterator.hasNext()) {
                                final String next = iterator.peek();
                                if (mobSplit.get(1).equals(next.replace(".", ""))) {
                                    if (mobSplit.size() > 2) {
                                        if (iterator.hasNext()) {
                                            final String nextNext = iterator.peek2();
                                            if (mobSplit.get(2).equals(nextNext.replace(".", ""))) {
                                                iterator.next();
                                                iterator.next();
                                                killer = new Killer(mobType, KillerType.MOB);
                                                continue OUTER_LOOP;
                                            }
                                        }
                                    } else {
                                        iterator.next();
                                        killer = new Killer(mobType, KillerType.MOB);
                                        continue OUTER_LOOP;
                                    }
                                }
                            }
                        } else {
                            killer = new Killer(mobType, KillerType.MOB);
                            continue OUTER_LOOP;
                        }
                    }
                }
                return Optional.empty();
            } else {
                if (schemaWord.equals("a(n)")) {
                    // special case for grammar
                    if (!(mcTextWord.equals("a") || mcTextWord.equals("an"))) {
                        return Optional.empty();
                    }
                } else if (schemaWord.equals("(with)")) {
                    // special case for "with"/"using" alt shorthand
                    if (!(mcTextWord.equals("with") || mcTextWord.equals("using"))) {
                        return Optional.empty();
                    }
                } else {
                    if (!mcTextWord.equals(schemaWord)) {
                        return Optional.empty();
                    }
                }
            }
        }
        if (isNull(victim) || iterator.hasNext()) return Optional.empty(); // we shouldn't ever reach this but just in case
        if (playerNames.size() == 2 && (killer == null || killer.type() != KillerType.PLAYER)) {
            DEFAULT_LOG.warn("Expected player killer but found: {}", killer);
        } else if (playerNames.size() == 1 && (killer != null && killer.type() == KillerType.PLAYER)) {
            DEFAULT_LOG.warn("Expected no player killer but found: {}", killer);
        }
        return Optional.of(new DeathMessageParseResult(victim, Optional.ofNullable(killer), Optional.ofNullable(weapon), this));
    }
}
