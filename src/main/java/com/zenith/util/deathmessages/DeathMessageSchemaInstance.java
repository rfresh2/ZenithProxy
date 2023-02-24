package com.zenith.util.deathmessages;

import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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

    public Optional<DeathMessageParseResult> parse(final String deathMessageRaw) {
        /**
         * todo: we need to rework something to allow for multiple schema matches
         *  e.g. $v was slain by $m. VS $v was slain by $m wielding $w
         *  Both will match but we obv want to the longer one
         *  some edge cases around $m and $w since these can be multiple words
         */

        final WordIterator iterator = new WordIterator(deathMessageRaw);
        String victim = null;
        Killer killer = null;
        String weapon = null;

        OUTER_LOOP:
        for (int i = 0; i < schema.size(); i++) {
            final String schemaWord = schema.get(i);
            final String mcTextWord = iterator.next();
            if (isNull(mcTextWord)) return Optional.empty();
            if (schemaWord.startsWith("$v")) {
                if (!userNameValidPattern.matcher(mcTextWord).matches()) {
                    return Optional.empty();
                } else {
                    victim = mcTextWord;
                }
            } else if (schemaWord.startsWith("$k")) {
                if (!userNameValidPattern.matcher(mcTextWord).matches()) {
                    return Optional.empty();
                } else {
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
                        if (next.equals(schemaPeek)) {
                            i++;
                            continue OUTER_LOOP;
                        } else {
                            weaponWip += " " + next;
                        }
                    }
                    weapon = weaponWip;
                } else {
                    String weaponWip = mcTextWord;
                    while (iterator.hasNext()) {
                        final String next = iterator.next();
                        weaponWip += " " + next;
                    }
                    weapon = weaponWip;
                }
            } else if (schemaWord.contains("$m")) { // iterator doesn't split these out
                if (mcTextWord.equals("a") || mcTextWord.equals("an")) {
                    i--; // skips any a or an prefix to a mob type
                    continue;
                }

                for (final String mobType : this.mobs) {
                    final List<String> mobSplit = spaceSplit(mobType);
                    if (mobSplit.get(0).equals(mcTextWord.replace(".", ""))) {
                        if (mobSplit.size() > 1) {
                            if (iterator.hasNext()) {
                                final String next = iterator.next();
                                if (mobSplit.get(1).equals(next.replace(".", ""))) {
                                    killer = new Killer(mobType, KillerType.MOB);
                                    continue OUTER_LOOP;
                                }
                            } else {
                                return Optional.empty();
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
        if (isNull(victim)) return Optional.empty(); // we shouldn't ever reach this but just in case
        return Optional.of(new DeathMessageParseResult(victim, Optional.ofNullable(killer), Optional.ofNullable(weapon), this));
    }
}
