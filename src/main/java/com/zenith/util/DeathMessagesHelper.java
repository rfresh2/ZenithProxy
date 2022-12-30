package com.zenith.util;

import com.google.common.io.Files;
import lombok.Data;
import net.daporkchop.lib.logging.format.component.TextComponent;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CLIENT_LOG;
import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

// todo: simplify and refactor this class
//  i suspect there's some regex hack that can vastly simplify all this parsing
public class DeathMessagesHelper {
    List<DeathMessageSchemaInstance> deathMessageSchemaInstances;

    public DeathMessagesHelper() {
        initSchemas();
    }

    public Optional<DeathMessageParseResult> parse(final MCTextRoot mcTextRoot) {
        if (nonNull(deathMessageSchemaInstances)) {
            for (final DeathMessageSchemaInstance instance : deathMessageSchemaInstances) {
                final Optional<DeathMessageParseResult> parse = instance.parse(mcTextRoot);
                if (parse.isPresent()) {
                    return parse;
                }
            }
        }
        DATABASE_LOG.error("No death message schema found for '{}'", mcTextRoot.toRawString());
        return Optional.empty();
    }

    private void initSchemas() {
        try {
            if (isNull(deathMessageSchemaInstances)) {
                final File file = Paths.get(getClass().getClassLoader().getResource("death_messages.txt").toURI()).toFile();
                final List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
                deathMessageSchemaInstances = lines.stream()
                        .filter(l -> !l.isEmpty()) //any empty lines
                        .filter(l -> !l.startsWith("#")) //comments
                        .map(DeathMessageSchemaInstance::new)
                        .collect(Collectors.toList());
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death messages", e);
        }
    }

    public static final class DeathMessageSchemaInstance {
        String schemaRaw;
        List<String> schema;

        public DeathMessageSchemaInstance(final String schemaRaw) {
            this.schemaRaw = schemaRaw;
            this.schema = Arrays.asList(schemaRaw.split(" "));
        }

        public Optional<DeathMessageParseResult> parse(final MCTextRoot mcTextRoot) {
            final MCTextRootIterator iterator = new MCTextRootIterator(mcTextRoot);
            String victim = null;
            String killer = null;
            String weapon = null;
            for (int i = 0; i < schema.size() && iterator.hasNext(); i++) {
                final String s = schema.get(i);
                final MCTextWord word = iterator.next();
                if (s.startsWith("$v")) {
                    if (!word.isKeyword) {
                        return Optional.empty();
                    } else {
                        victim = word.word;
                    }
                } else if (s.startsWith("$k")) {
                    if (!word.isKeyword) {
                        return Optional.empty();
                    } else {
                        killer = word.word;
                    }
                } else if (s.startsWith("$w")) {
                    if (!word.isKeyword) {
                        return Optional.empty();
                    } else {
                        weapon = word.word;
                    }
                } else {
                    if (word.isKeyword) {
                        return Optional.empty();
                    } else {
                        if (s.equals("a(n)")) {
                            // special case for grammar
                            if (!(word.word.equals("a") || word.word.equals("an"))) {
                                return Optional.empty();
                            }
                        } else {
                            if (!word.word.equals(s)) {
                                return Optional.empty();
                            }
                        }
                    }
                }
            }
            if (isNull(victim)) return Optional.empty();
            return Optional.of(new DeathMessageParseResult(victim, Optional.ofNullable(killer), Optional.ofNullable(weapon), this));
        }
    }

    @Data
    public static final class DeathMessageParseResult {
        final String victim;
        final Optional<String> killer;
        final Optional<String> weapon;
        final DeathMessageSchemaInstance deathMessageSchemaInstance;
    }

    public static final class MCTextRootIterator implements Iterator<MCTextWord> {
        final MCTextRoot mcTextRoot;
        private int childIndex = 0;
        private int wordIndex = 0;

        public MCTextRootIterator(final MCTextRoot mcTextRoot) {
            this.mcTextRoot = mcTextRoot;
        }

        @Override
        public boolean hasNext() {
            // todo: this is kinda hacky
            final int beforeChildIndex = childIndex;
            final int beforeWordIndex = wordIndex;
            final MCTextWord next = next();
            childIndex = beforeChildIndex;
            wordIndex = beforeWordIndex;
            if (isNull(next)) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public MCTextWord next() {
            if (mcTextRoot.getChildren().size() > childIndex) {
                final TextComponent childComponent = mcTextRoot.getChildren().get(childIndex);
                final List<String> words = Arrays.asList(childComponent.toRawString().split(" "));
                if (words.size() > wordIndex) {
                    boolean isKeyword = !childComponent.getColor().equals(new Color(170, 0, 0));
                    String word = words.get(wordIndex);
                    if (isKeyword) {
                        if (word.startsWith("'s")) { // special case where player names have possession modifier in a child
                            wordIndex++;
                            return next();
                        }
                        // weapons can have multiple words
                        word = String.join(" ", words.toArray(new String[0]));
                        wordIndex = Integer.MAX_VALUE;
                    } else {
                        wordIndex++;
                        if (word.isEmpty())
                            return next(); // if child words starts with " " then split will have an empty string here
                    }
                    return new MCTextWord(isKeyword, word);
                } else {
                    childIndex++;
                    wordIndex = 0;
                    return next();
                }
            } else {
                return null;
            }
        }
    }

    public static final class MCTextWord {
        boolean isKeyword;
        String word;

        public MCTextWord(final boolean isKeyword, final String word) {
            this.isKeyword = isKeyword;
            this.word = word;
        }
    }
}
