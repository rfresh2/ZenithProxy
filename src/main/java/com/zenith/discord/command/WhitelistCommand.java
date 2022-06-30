package com.zenith.discord.command;

import com.zenith.Proxy;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.zenith.util.Constants.*;

public class WhitelistCommand extends Command {
    public WhitelistCommand(Proxy proxy) {
        super(proxy, "whitelist", "Manage the proxy's whitelist. Only usable by users with the account owner role."
                + "\nUsage: whitelist add/del/list <username>");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        this.validateUserHasAccountOwnerRole(event, restChannel);
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() < 2) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("list")) {
            embedBuilder
                    .title("Whitelist List")
                    .color(Color.CYAN)
                    .addField("Whitelisted", ((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty"),
                            false);
        } else if (commandArgs.size() < 3) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("add")) {
            if (!CONFIG.server.extra.whitelist.allowedUsers.contains(commandArgs.get(2))) {
                CONFIG.server.extra.whitelist.allowedUsers.add(commandArgs.get(2));
            }
            embedBuilder
                    .title("Added user: " + commandArgs.get(2) + " To Whitelist")
                    .color(Color.CYAN)
                    .addField("Whitelisted", ((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty"),
                            false);
        } else if (commandArgs.get(1).equalsIgnoreCase("del")) {
            CONFIG.server.extra.whitelist.allowedUsers.removeIf(s -> s.equalsIgnoreCase(commandArgs.get(2)));
            embedBuilder
                    .title("Removed user: " + commandArgs.get(2) + " From Whitelist")
                    .color(Color.CYAN)
                    .addField("Whitelisted", ((CONFIG.server.extra.whitelist.allowedUsers.size() > 0) ? String.join(", ", CONFIG.server.extra.whitelist.allowedUsers) : "Whitelist is empty"),
                            false);
        } else {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        }

        saveConfig();
        return MessageCreateSpec.builder()
                .addEmbed(embedBuilder
                        .build())
                .build().asRequest();
    }
}
