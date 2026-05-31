package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.chatschema.ChatSchema;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;

public class ChatSchemaCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("chatSchema")
            .category(CommandCategory.MANAGE)
            .description("""
                Configure how ZenithProxy parses public chats and whispers.

                Correct schemas are needed for chat relay and chat based features to work correctly.

                Schemas have the following special tokens:
                * $s -> Chat/whisper sender, player name
                * $r -> Whisper receiver, player name
                * $m -> Message, text content of the chat/whisper
                * $w -> Wildcard, any varying text, e.g. a role prefix `[ADMIN] rfresh2: test message`

                Example 2b2t chat schema:
                * public chat: `<$s> $m`
                * whisper inbound: `$s whispers: $m`
                * whisper outbound: `to $r: $m`

                You can configure different schemas for different servers based on the server address.

                Server address is without port, e.g. `2b2t.org` or `192.168.0.5`
                """)
            .usageLines(
                "set <publicChat/whisperInbound/whisperOutbound> <serverAddress> <schema>",
                "preset <serverAddress> <2b2t/essentials>",
                "del <serverAddress>",
                "list"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("chatSchema").requires(Command::validateAccountOwner)
            .then(literal("del")
                .executes(c -> {
                    var serverAddress = CONFIG.client.server.address;
                    CONFIG.client.chatSchemas.serverSchemas.remove(serverAddress);
                    c.getSource().getEmbed()
                        .title("Server Removed");
                })
                .then(argument("serverAddress", wordWithChars()).executes(c -> {
                    var serverAddress = getString(c, "serverAddress");
                    CONFIG.client.chatSchemas.serverSchemas.remove(serverAddress);
                    c.getSource().getEmbed()
                        .title("Server Removed");
                })))
            .then(literal("set")
                .then(literal("publicChat").then(argument("serverAddress", wordWithChars()).then(argument("schema", greedyString()).executes(c -> {
                    var serverAddress = getString(c, "serverAddress").toLowerCase().trim();
                    var schemaArg = getString(c, "schema");
                    var originalSchema = CONFIG.client.chatSchemas.serverSchemas.getOrDefault(serverAddress, ChatSchema.DEFAULT_SCHEMA);
                    var newSchema = new ChatSchema(schemaArg, originalSchema.whisperInbound(), originalSchema.whisperOutbound());
                    CONFIG.client.chatSchemas.serverSchemas.put(serverAddress, newSchema);
                    c.getSource().getEmbed()
                        .title("Public Chat Schema Set");
                }))))
                .then(literal("whisperInbound").then(argument("serverAddress", wordWithChars()).then(argument("schema", greedyString()).executes(c -> {
                    var serverAddress = getString(c, "serverAddress").toLowerCase().trim();
                    var schemaArg = getString(c, "schema");
                    var originalSchema = CONFIG.client.chatSchemas.serverSchemas.getOrDefault(serverAddress, ChatSchema.DEFAULT_SCHEMA);
                    var newSchema = new ChatSchema(originalSchema.publicChat(), schemaArg, originalSchema.whisperOutbound());
                    CONFIG.client.chatSchemas.serverSchemas.put(serverAddress, newSchema);
                    c.getSource().getEmbed()
                        .title("Inbound Whisper Schema Set");
                }))))
                .then(literal("whisperOutbound").then(argument("serverAddress", wordWithChars()).then(argument("schema", greedyString()).executes(c -> {
                    var serverAddress = getString(c, "serverAddress").toLowerCase().trim();
                    var schemaArg = getString(c, "schema");
                    var originalSchema = CONFIG.client.chatSchemas.serverSchemas.getOrDefault(serverAddress, ChatSchema.DEFAULT_SCHEMA);
                    var newSchema = new ChatSchema(originalSchema.publicChat(), originalSchema.whisperInbound(), schemaArg);
                    CONFIG.client.chatSchemas.serverSchemas.put(serverAddress, newSchema);
                    c.getSource().getEmbed()
                        .title("Outbound Whisper Schema Set");
                })))))
            .then(literal("preset").then(argument("serverAddress", wordWithChars())
                .then(literal("2b2t").executes(c -> {
                    var serverAddress = getString(c, "serverAddress").toLowerCase().trim();
                    var schema2b2t = new ChatSchema(
                        "<$s> $m",
                        "$s whispers: $m",
                        "to $r: $m"
                    );
                    CONFIG.client.chatSchemas.serverSchemas.put(serverAddress, schema2b2t);
                    c.getSource().getEmbed()
                        .title("2b2t Schema Set");
                }))
                .then(literal("essentials").executes(c -> {
                    var serverAddress = getString(c, "serverAddress").toLowerCase().trim();
                    var schemaEssentials = new ChatSchema(
                        "<$s> $m",
                        "[$s -> me] $m",
                        "[me -> $r] $m"
                    );
                    CONFIG.client.chatSchemas.serverSchemas.put(serverAddress, schemaEssentials);
                    c.getSource().getEmbed()
                        .title("Essentials Schema Set");
                }))))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Chat Schemas");
            }));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .description(getSchemaList())
            .primaryColor();
    }

    private String getSchemaList() {
        StringBuilder list = new StringBuilder();
        list.append("**Default Schema**\n");
        list.append(printSchema(ChatSchema.DEFAULT_SCHEMA));
        if (!CONFIG.client.chatSchemas.serverSchemas.isEmpty()) {
            for (var serverSchemaEntries : CONFIG.client.chatSchemas.serverSchemas.entrySet()) {
                var serverAddress = serverSchemaEntries.getKey();
                var schema = serverSchemaEntries.getValue();
                list.append("**Server: %s**\n".formatted(serverAddress));
                list.append(printSchema(schema));
            }
        }
        return list.toString();
    }

    private String printSchema(ChatSchema schema) {
        return """
            * Public Chat: `%s`
            * Whisper Inbound: `%s`
            * Whisper Outbound: `%s`
            """.formatted(
            schema.publicChat(),
            schema.whisperInbound(),
            schema.whisperOutbound()
        );
    }
}
