package com.zenith;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.generator.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;

public class DataGenerator implements DedicatedServerModInitializer {
    public static Logger LOG = LoggerFactory.getLogger("ZenithProxy");
    public static Path dataDir = resolveDataDir();
    public static Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .disableHtmlEscaping()
        .create();
    public static List<Generator> generators = asList(
        new BlockCollisionShapes(),
        new Blocks(),
        new BlockToMapColorId(),
        new Entities(),
        new Food(),
        new Items(),
        new Language(),
        new MapColorIdToColor()
    );
    public static MinecraftServer SERVER_INSTANCE;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            LOG.info("Server started!");
            SERVER_INSTANCE = server;
            generators.forEach(Generator::generate);
            LOG.info("Data generation complete!");
            Runtime.getRuntime().halt(0);
        });
    }

    private static Path resolveDataDir() {
        var dataDir = System.getProperty("data.dir");
        if (dataDir == null) {
            throw new RuntimeException("data.dir system property not set");
        }
        Path path = Paths.get(dataDir);
        LOG.info("Data dir: " + path);
        path.toFile().mkdirs();
        // clear all files inside the data dir
        for (File file : path.toFile().listFiles()) {
            file.delete();
        }
        return path;
    }

    public static File outputFile(final String fileName) {
        return dataDir.resolve(fileName).toFile();
    }
}
