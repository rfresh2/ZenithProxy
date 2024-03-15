package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.map.MapGenerator;
import com.zenith.feature.map.MapRenderer;
import discord4j.rest.util.Color;

import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

public class MapCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "map",
            CommandCategory.INFO, """
    Generate and render map images.
    Map ID's to render must be cached during the current session
    """,
            asList(
                "render <mapId>",
                "generate",
                "generate <viewDistance>"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("map")
            .then(literal("render")
                .then(argument("mapId", integer()).executes(c -> {
                    final int id = c.getArgument("mapId", Integer.class);
                    var mapData = CACHE.getMapDataCache().getMapDataMap().get(id);
                    if (mapData == null) {
                        var knownIdList = CACHE.getMapDataCache().getMapDataMap().keySet().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ", "[", "]"));
                        c.getSource().getEmbed()
                            .title("Map Not Found")
                            .description("**Known Map ID's**\n" + knownIdList)
                            .addField("Map ID", id, true)
                            .color(Color.RUBY);
                        return 1;
                    }
                    var bytes = MapRenderer.render(mapData.getData(), id);
                    var attachmentName = "map_" + id + ".png";
                    c.getSource().getEmbed()
                        .title("Map Rendered!")
                        .addField("Map ID", id, true)
                        .fileAttachment(new Embed.FileAttachment(
                            attachmentName,
                            bytes
                        ))
                        .image("attachment://" + attachmentName)
                        .color(Color.CYAN);
                    return 1;
                })))
            .then(literal("generate")
                      .executes(c -> {
                          var bytes = MapRenderer.render(MapGenerator.generateMapData(), -1);
                          var attachmentName = "map.png";
                          c.getSource().getEmbed()
                              .title("Map Generated!")
                              .fileAttachment(new Embed.FileAttachment(
                                  attachmentName,
                                  bytes
                              ))
                              .image("attachment://" + attachmentName)
                              .color(Color.CYAN);
                          return 1;
                      })
                      .then(argument("viewDistance", integer(1, 16)).executes(c -> {
                          var viewDistance = c.getArgument("viewDistance", Integer.class);
                          var chunkSquareWidth = viewDistance * 2;
                          var blockSquareWidth = chunkSquareWidth * 16;
                          var bytes = MapRenderer.render(MapGenerator.generateMapData(blockSquareWidth), -1, blockSquareWidth);
                          var attachmentName = "map.png";
                          c.getSource().getEmbed()
                              .title("Map Generated!")
                              .fileAttachment(new Embed.FileAttachment(
                                  attachmentName,
                                  bytes
                              ))
                              .image("attachment://" + attachmentName)
                              .color(Color.CYAN);
                          return 1;
                      })));
    }
}
