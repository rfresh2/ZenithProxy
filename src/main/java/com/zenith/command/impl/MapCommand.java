package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.feature.map.MapGenerator;
import com.zenith.feature.map.MapRenderer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CACHE;
import static java.util.Arrays.asList;

public class MapCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "map",
            CommandCategory.INFO,
    """
            Generate and render map images.
            Map ID's to render must be cached during the current session
            Generated maps can optionally be aligned to the vanilla map grid, or generated with a custom view distance.
            Generated maps cannot be larger than what chunks are currently cached in the proxy
            """,
            asList(
                "render <mapId>",
                "render all",
                "generate",
                "generate align",
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
                                  .errorColor();
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
                              .primaryColor();
                          return 1;
                      }))
                      .then(literal("all").executes(c -> {
                          final AtomicInteger count = new AtomicInteger(0);
                          CACHE.getMapDataCache().getMapDataMap().forEach((id, mapData) -> {
                              MapRenderer.render(mapData.getData(), id);
                              count.incrementAndGet();
                          });
                          c.getSource().getEmbed()
                              .title("All Cached Maps Rendered")
                              .primaryColor()
                              .addField("Map Count", count.get(), false);
                          return 1;
                      })))
            .then(literal("generate")
                      .executes(c -> {
                          generate(c.getSource().getEmbed(), 4, false);
                          return 1;
                      })
                      .then(literal("align").executes(c -> {
                          generate(c.getSource().getEmbed(), 4, true);
                          return 1;
                      }))
                      .then(argument("viewDistance", integer(1, 16)).executes(c -> {
                          var viewDistance = c.getArgument("viewDistance", Integer.class);
                          generate(c.getSource().getEmbed(), viewDistance, false);
                          return 1;
                      })));
    }

    private void generate(final Embed embed, final int viewDistance, final boolean vanillaAlign) {
        final int chunkSquareWidth = viewDistance * 2;
        final int blockSquareWidth = chunkSquareWidth * 16;
        var bytes = MapRenderer.render(MapGenerator.generateMapData(blockSquareWidth, vanillaAlign), -1, blockSquareWidth);
        var attachmentName = "map.png";
        embed
            .title("Map Generated!")
            .fileAttachment(new Embed.FileAttachment(
                attachmentName,
                bytes
            ))
            .image("attachment://" + attachmentName)
            .primaryColor();
    }
}
