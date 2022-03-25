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

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.saveConfig;

public class VisualRangeCommand extends Command {

    public VisualRangeCommand(Proxy proxy) {
        super(proxy, "visualRange", "Configure the VisualRange notification feature"
                + "\nUsage:"
                + "\n  " + CONFIG.discord.prefix + "visualRange on/off");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        List<String> commandArgs = Arrays.asList(event.getMessage().getContent().split(" "));
        EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder();

        if (commandArgs.size() < 1) {
            embedBuilder
                    .title("Invalid command usage")
                    .addField("Usage", this.description, false)
                    .color(Color.RUBY);
        } else if (commandArgs.get(1).equalsIgnoreCase("on")) {
            CONFIG.client.extra.visualRangeAlert = true;
            embedBuilder
                    .title("VisualRange Enabled!")
                    .color(Color.CYAN);
        } else if (commandArgs.get(1).equalsIgnoreCase("off")) {
            CONFIG.client.extra.visualRangeAlert = false;
            embedBuilder
                    .title("VisualRange Disabled!")
                    .color(Color.CYAN);
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
                .build().asRequest();    }
}
