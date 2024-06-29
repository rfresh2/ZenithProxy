package com.zenith.feature.deathmessages;

import com.zenith.discord.Embed;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.zenith.Shared.*;
import static com.zenith.feature.deathmessages.DeathMessageSchemaInstance.spaceSplit;

public class DeathMessagesParser {
    private final List<DeathMessageSchemaInstance> deathMessageSchemaInstances;
    private final List<String> mobs;

    public DeathMessagesParser() {
        List<String> mobsTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("death_message_mobs.schema")))) {
                mobsTemp = br.lines()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toList());
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing mobs for death message parsing", e);
        }
        mobs = mobsTemp;
        List<DeathMessageSchemaInstance> schemaInstancesTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("death_messages.schema")))) {
                schemaInstancesTemp = br.lines()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .map(l -> new DeathMessageSchemaInstance(l, mobs))
                    .collect(Collectors.toList());
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death message schemas", e);
        }
        deathMessageSchemaInstances = schemaInstancesTemp;
    }

    public Optional<DeathMessageParseResult> parse(final Component component, final String rawInput) {
        List<String> playerNames = getPlayerNames(component);
        List<String> inputSplit = spaceSplit(rawInput);
        for (int i = 0; i < deathMessageSchemaInstances.size(); i++) {
            final DeathMessageSchemaInstance instance = deathMessageSchemaInstances.get(i);
            final Optional<DeathMessageParseResult> parse = instance.parse(inputSplit, playerNames);
            if (parse.isPresent()) return parse;
        }
        if (CONFIG.database.deaths.enabled && CONFIG.database.deaths.unknownDeathDiscordMsg && DISCORD.isRunning()) {
            DISCORD.sendEmbedMessage(Embed.builder()
                                         .title("Unknown death message")
                                         .description(ComponentSerializer.serializeJson(component))
                                         .addField("Message", rawInput, false)
                                         .errorColor());
        }
        DEFAULT_LOG.warn("No death message schema found for '{}'", rawInput);
        return Optional.empty();
    }

    List<String> getPlayerNames(final Component component) {
        return component.children().stream()
            .map(Component::clickEvent)
            .filter(Objects::nonNull)
            .filter(clickEvent -> clickEvent.action() == ClickEvent.Action.SUGGEST_COMMAND && clickEvent.value().startsWith("/w"))
            .map(clickEvent -> clickEvent.value().substring(3).trim())
            .toList();
    }
}
