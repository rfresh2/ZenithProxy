package com.zenith.discord.command.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.zenith.Proxy;
import com.zenith.cache.data.tab.PlayerEntry;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zenith.util.Constants.CACHE;
import static com.zenith.util.Constants.DEFAULT_LOG;

public class TablistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.of(
                "tablist",
                "Displays the current server's tablist",
                Collections.emptyList()
        );
    }

    @Override
    public void register(CommandDispatcher<CommandContext> dispatcher) {
        dispatcher.register(
                command("tablist").executes(c -> {
                    if (!Proxy.getInstance().isConnected()) {
                        c.getSource().getEmbedBuilder()
                                .title("Proxy is not online!")
                                .color(Color.RUBY);
                    } else {
                        // embeds will be too small for tablist
                        List<String> playerNames = CACHE.getTabListCache().getTabList().getEntries().stream()
                                .map(PlayerEntry::getName)
                                .distinct()
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
                                c.getSource().getRestChannel().createMessage("```\n" + outputMessage + "\n```").block();
                            });
                        } catch (final Exception e) {
                            DEFAULT_LOG.error("", e);
                        }
                    }
                })
        );
    }
}
