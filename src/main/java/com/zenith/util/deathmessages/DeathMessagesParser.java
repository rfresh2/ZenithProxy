package com.zenith.util.deathmessages;

import com.google.common.io.Files;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.CLIENT_LOG;
import static com.zenith.util.Constants.DATABASE_LOG;
import static java.util.Objects.nonNull;

// todo: simplify and refactor this class
//  i suspect there's some regex hack that can vastly simplify all this parsing
public class DeathMessagesParser {
    final List<DeathMessageSchemaInstance> deathMessageSchemaInstances;

    public DeathMessagesParser() {
        List<DeathMessageSchemaInstance> schemaInstancesTemp = null;
        try {
            final File file = Paths.get(getClass().getClassLoader().getResource("death_messages.txt").toURI()).toFile();
            final List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
            schemaInstancesTemp = lines.stream()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .map(DeathMessageSchemaInstance::new)
                    .collect(Collectors.toList());

        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death messages", e);
        }
        deathMessageSchemaInstances = schemaInstancesTemp;
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
}
