package com.zenith.discord.command;

import com.google.common.collect.Lists;
import com.zenith.Proxy;
import com.zenith.cache.data.tab.PlayerEntry;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.DEFAULT_LOG;

public class TablistCommand extends Command {
    public TablistCommand(Proxy proxy) {
        super(proxy, "tablist", "Displays the current server's tablist");
    }

    @Override
    public MultipartRequest<MessageCreateRequest> execute(MessageCreateEvent event, RestChannel restChannel) {
        if (!proxy.isConnected()) {
            return MessageCreateSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .title("Proxy is not online!")
                            .color(Color.RUBY)
                            .build())
                    .build().asRequest();
        } else {
            // embeds will be too small for tablist
            List<String> playerNames = CACHE.getTabListCache().getTabList().getEntries().stream()
                    .map(PlayerEntry::getName)
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
            final int longestPlayerNameSize = playerNames.stream().map(String::length).max(Integer::compareTo).get();
            final int colSize = 4; // num cols of playernames
            final int paddingSize = 1; // num spaces between names
            List<List<String>> playerNamesColumnized = Lists.partition(playerNames, colSize);
            final List<String> rows = new ArrayList<>();

            final List<List<String>> colOrderedNames = new ArrayList<>();
            IntStream.range(0, playerNamesColumnized.size())
                    .forEach(i -> colOrderedNames.add(new ArrayList<>()));
            // iterate down col, then row
            final ListIterator<String> pNameIterator = playerNames.listIterator();
            for (int i = 0; i < colSize; i++) {
                for (int col = 0; col < playerNamesColumnized.size(); col++) {
                    if (pNameIterator.hasNext()) {
                        colOrderedNames.get(col).add(pNameIterator.next());
                    } else {
                        break;
                    }
                }
            }

            colOrderedNames.forEach(row -> {
                final StringBuilder stringBuilder = new StringBuilder();
                final Formatter formatter = new Formatter(stringBuilder);
                final String formatting = IntStream.range(0, row.size())
                        .mapToObj(i -> "%-" + (longestPlayerNameSize + paddingSize) + "." + (longestPlayerNameSize + paddingSize) + "s")
                        .collect(Collectors.joining(" "));
                formatter.format(formatting, row.toArray());
                stringBuilder.append("\n");
                rows.add(stringBuilder.toString());
            });


            final List<String> outputMessages = new ArrayList<>();
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < rows.size(); i++) {
                if (out.toString().length() + rows.get(i).length() < 1950) {
                    out.append(rows.get(i));
                } else {
                    outputMessages.add(out.toString());
                    out = new StringBuilder();
                }
            }
            outputMessages.add(out.toString());
            try {
                outputMessages.forEach(outputMessage -> {
                    restChannel.createMessage("```\n" + outputMessage + "\n```").block();
                });
            } catch (final Exception e) {
                DEFAULT_LOG.error(e);
            }
            return null;
        }
    }
}
