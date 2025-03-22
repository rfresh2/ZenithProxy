package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class RateLimiterCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("rateLimiter")
            .category(CommandCategory.MODULE)
            .description("""
              Limits how often players are allowed to attempt logins and send packets.
              
              The rate limit is counted as the number of seconds between logins allowed.
              The login rate limit is per IP address.
              
              The packet rate limiter is counted per connection.
              Packets received are counted in a configurable interval of seconds.
              If more packets than the rateLimit are received in that interval, the player is disconnected.
              """)
            .usageLines(
                "login on/off",
                "login rateLimit <seconds>",
                "packet on/off",
                "packet interval <seconds>",
                "packet rateLimit <int>"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("rateLimiter")
            .then(literal("login")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.loginRateLimiter.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Login Rate Limiter " + toggleStrCaps(CONFIG.server.loginRateLimiter.enabled));
                          return OK;
                      }))
                      .then(literal("rateLimit").then(argument("seconds", integer(0, 1000)).executes(c -> {
                          CONFIG.server.loginRateLimiter.rateLimitSeconds = getInteger( c, "seconds");
                          c.getSource().getEmbed()
                              .title("Login Rate Limit Set")
                              .description("Changes will take effect on next ZenithProxy restart.");
                          return OK;
                      }))))
            .then(literal("packet")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.packetRateLimiter.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Packet Rate Limiter " + toggleStrCaps(CONFIG.server.packetRateLimiter.enabled));
                          return OK;
                      }))
                      .then(literal("interval").then(argument("packetInterval", doubleArg(0.05, 1000)).executes(c -> {
                          CONFIG.server.packetRateLimiter.intervalSeconds = getDouble(c, "packetInterval");
                          c.getSource().getEmbed()
                              .title("Packet Interval Set")
                              .description("Changes will take effect on next ZenithProxy restart.");
                          return OK;
                      })))
                      .then(literal("rateLimit").then(argument("rateLimitSeconds", integer(1, 1_000_000)).executes(c -> {
                          CONFIG.server.packetRateLimiter.maxPacketsPerInterval = getInteger( c, "rateLimitSeconds");
                          c.getSource().getEmbed()
                              .title("Packet Rate Limit Set");
                          return OK;
                      }))));
    }

    @Override
    public void postPopulate(Embed embed) {
        embed
            .addField("Login", toggleStr(CONFIG.server.loginRateLimiter.enabled))
            .addField("Login Rate Limit", CONFIG.server.loginRateLimiter.rateLimitSeconds + "s")
            .addField("Packet", toggleStr(CONFIG.server.packetRateLimiter.enabled))
            .addField("Packet Interval", CONFIG.server.packetRateLimiter.intervalSeconds + "s")
            .addField("Packet Rate Limit", toggleStr(CONFIG.server.packetRateLimiter.enabled))
            .primaryColor();
    }
}
