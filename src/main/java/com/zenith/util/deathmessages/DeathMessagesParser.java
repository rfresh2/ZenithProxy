package com.zenith.util.deathmessages;

import com.google.common.io.Files;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.*;
import static java.util.Objects.nonNull;

public class DeathMessagesParser {
    private final List<DeathMessageSchemaInstance> deathMessageSchemaInstances;
    private final List<String> mobs;

    public DeathMessagesParser() {
        List<String> mobsTemp = Collections.emptyList();
        try {
            final File file = Paths.get(getClass().getClassLoader().getResource("death_message_mobs.schema").toURI()).toFile();
            final List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
            mobsTemp = lines.stream()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing mobs for death message parsing", e);
        }
        mobs = mobsTemp;
        List<DeathMessageSchemaInstance> schemaInstancesTemp = Collections.emptyList();
        try {
            final File file = Paths.get(getClass().getClassLoader().getResource("death_messages.schema").toURI()).toFile();
            final List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
            schemaInstancesTemp = lines.stream()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .map(l -> new DeathMessageSchemaInstance(l, mobs))
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death message schemas", e);
        }
        deathMessageSchemaInstances = schemaInstancesTemp;
    }

    public Optional<DeathMessageParseResult> parse(final String rawInput) {
        if (nonNull(deathMessageSchemaInstances)) {
            for (final DeathMessageSchemaInstance instance : deathMessageSchemaInstances) {
                final Optional<DeathMessageParseResult> parse = instance.parse(rawInput);
                if (parse.isPresent()) {
                    return parse;
                }
            }
        }
        if (CONFIG.database.deaths.unknownDeathDiscordMsg && DISCORD_BOT.isRunning()) {
            DISCORD_BOT.sendEmbedMessage(EmbedCreateSpec.builder()
                    .title("Unknown death message")
                    .description(rawInput)
                    .color(Color.RUBY)
                    .build());
        }
        DATABASE_LOG.error("No death message schema found for '{}'", rawInput);
        return Optional.empty();
    }
}
