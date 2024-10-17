package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static java.util.Arrays.asList;

public class ReleaseChannelCommand extends Command {
    private static final List<String> PLATFORMS = asList("java", "linux");
    private static final List<String> MINECRAFT_VERSIONS = asList("1.12.2", "1.20.1", "1.20.4", "1.20.6", "1.21.0", "1.21.2");

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "channel",
            CommandCategory.MANAGE,
            """
            Configures the current AutoUpdater release channel.
            
            The release channel is a combination of a platform (java or linux) and a Minecraft protocol version.
            .""",
            asList(
                "list",
                "set <platform> <minecraft version>"
            ),
            asList(
                "release",
                "releaseChannel"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("channel")
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Release Channel Info")
                    .addField("Current Release Channel", LAUNCH_CONFIG.release_channel, true)
                    .addField("Available Platforms", PLATFORMS.stream().collect(Collectors.joining("`, `", "`", "`")), false)
                    .addField("Available Minecraft Versions", MINECRAFT_VERSIONS.stream().collect(Collectors.joining("`, `", "`", "`")), false)
                    .primaryColor();
            }))
            .then(literal("set")
                      .then(argument("channel", wordWithChars())
                                .then(argument("minecraft_version", wordWithChars()).executes(c -> {
                                    final String channel = StringArgumentType.getString(c, "channel");
                                    final String minecraft_version = StringArgumentType.getString(c, "minecraft_version");
                                    setChannel(c, channel, minecraft_version, false);
                                    return OK;
                                })
                                          .then(literal("pre").executes(c -> {
                                              final String channel = StringArgumentType.getString(c, "channel");
                                              final String minecraft_version = StringArgumentType.getString(c, "minecraft_version");
                                              setChannel(c, channel, minecraft_version, true);
                                              return OK;
                                          })))));
    }

    private void setChannel(com.mojang.brigadier.context.CommandContext<CommandContext> c, String channel, String minecraft_version, boolean pre) {
        if (!PLATFORMS.contains(channel)) {
            c.getSource().getEmbed()
                .title("Invalid Platform!")
                .description("Available platforms: " + PLATFORMS)
                .errorColor();
            return;
        }
        if (!MINECRAFT_VERSIONS.contains(minecraft_version)) {
            c.getSource().getEmbed()
                .title("Invalid Minecraft Version!")
                .description("Available versions: " + MINECRAFT_VERSIONS)
                .errorColor();
            return;
        }
        if (channel.equals("linux")) {
            if (!validateLinuxPlatform()) {
                c.getSource().getEmbed()
                    .title("Invalid Platform!")
                    .description("Invalid system for linux channel")
                    .errorColor();
                return;
            }
        }
        LAUNCH_CONFIG.release_channel = channel + "." + minecraft_version;
        if (pre) LAUNCH_CONFIG.release_channel += ".pre";
        c.getSource().getEmbed()
            .title("Release Channel Updated!")
            .addField("Release Channel", LAUNCH_CONFIG.release_channel, false)
            .addField("Info", "Please restart ZenithProxy for changes to take effect.\nOr apply now: `update`", false)
            .primaryColor();
        saveLaunchConfig();
    }

    private static final List<String> amd64CpuFlags = List.of("avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3");

    private boolean validateLinuxPlatform() {
        if (LAUNCH_CONFIG.release_channel.startsWith("linux")) {
            // we're already on linux, so it must be ok
            return true;
        }

        // Check if we're running on linux OS
        boolean isLinuxOs = System.getProperty("os.name").toLowerCase().contains("linux");
        if (!isLinuxOs) {
            DEFAULT_LOG.warn("Linux release channel selected but not running on Linux OS.");
            return false;
        }

        // check cpu is x86_64
        boolean isAmd64Cpu = System.getProperty("os.arch").equals("amd64");
        if (!isAmd64Cpu) {
            DEFAULT_LOG.warn("Linux release channel selected but not running on x86_64 CPU.");
            return false;
        }
        // check if we have the required CPU flags
        try (Stream<String> linesStream = Files.lines(Paths.get("/proc/cpuinfo"))) {
            List<String> flags = linesStream
                .filter(line -> line.startsWith("flags"))
                .map(line -> line.split(":")[1].trim().split(" "))
                .flatMap(Arrays::stream)
                .toList();
            for (String reqFlag : amd64CpuFlags) {
                if (!flags.contains(reqFlag)) {
                    DEFAULT_LOG.warn("Linux release channel selected but CPU does not have required flag: {}", reqFlag);
                    return false;
                }
            }
        } catch (final Throwable e) {
            DEFAULT_LOG.warn("Error validating linux channel. Failed to read /proc/cpuinfo", e);
            return false;
        }

        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("ldd",  "--version");
            processBuilder.redirectErrorStream(true);

            final Process process = processBuilder.start();
            final StringBuilder stream = readStream(process.getInputStream());
            var output = stream.toString();
            String[] lines = output.split(System.lineSeparator());
            // ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
            // get the version from the last word of the first line
            String[] firstLineWordSplit = lines[0].split(" ");
            String version = firstLineWordSplit[firstLineWordSplit.length - 1];
            String[] versionSplit = version.split("\\.");
            int major = Integer.parseInt(versionSplit[0]);
            int minor = Integer.parseInt(versionSplit[1]);
            if (major != 2) {
                DEFAULT_LOG.warn("Linux release channel selected but glibc version is less than 2.31");
                return false;
            }
            var mcVersion = LAUNCH_CONFIG.getMcVersion();
            var oldVersions = Set.of("1.12.2", "1.20.1", "1.20.4", "1.20.6", "1.21.0");
            var minorVersionMin = mcVersion == null || oldVersions.contains(mcVersion) ? 31 : 35;
            if (minor < minorVersionMin) {
                DEFAULT_LOG.warn("Linux release channel selected but glibc version is less than 2.{}", minorVersionMin);
                return false;
            }
        } catch (final Throwable e) {
            DEFAULT_LOG.warn("Error validating linux channel. Failed to determine glibc version", e);
            return false;
        }
        return true;
    }

    private static StringBuilder readStream(InputStream iStream) throws IOException {
        final StringBuilder builder = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(iStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
        }
        return builder;
    }
}
