package com.zenith.command.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.DEFAULT_LOG;

public class TablistCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "tablist",
            CommandCategory.INFO,
            "Displays the current server's tablist"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("tablist").executes(c -> {
            if (!Proxy.getInstance().isConnected()) {
                c.getSource().getEmbed()
                    .title("Proxy is not online!")
                    .errorColor();
            } else {
                // embeds will be too small for tablist
                List<String> playerNames = CACHE.getTabListCache().getEntries().stream()
                    .map(PlayerListEntry::getName)
                    .distinct()
                    .sorted(String::compareTo)
                    .collect(Collectors.toList());
                final int longestPlayerNameSize = playerNames.stream().map(String::length).max(Integer::compareTo).orElse(1);
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
                        c.getSource().getMultiLineOutput().add("```\n" + outputMessage + "\n```");
                    });
                } catch (final Exception e) {
                    DEFAULT_LOG.error("", e);
                }
            }
        });
    }
}
