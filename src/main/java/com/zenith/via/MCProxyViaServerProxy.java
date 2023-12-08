package com.zenith.via;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.viaversion.viaversion.api.configuration.ViaVersionConfig;
import com.viaversion.viaversion.api.platform.PlatformTask;
import com.viaversion.viaversion.api.platform.ProtocolDetectorService;
import com.viaversion.viaversion.api.platform.ViaServerProxyPlatform;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.VersionInfo;
import com.viaversion.viaversion.velocity.util.LoggerWrapper;
import com.zenith.network.client.ClientSession;
import com.zenith.via.handler.MCProxyViaChannelInitializer;
import com.zenith.via.platform.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

import static com.zenith.Shared.SCHEDULED_EXECUTOR_SERVICE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MCProxyViaServerProxy implements ViaServerProxyPlatform<MinecraftProtocol> {
    private final ProtocolDetectorService protocolDetectorService = new MCProxyProtocolDetectorService();
    private final MCProxyViaAPI api = new MCProxyViaAPI();
    private final MCProxyViaConfig config = new MCProxyViaConfig(Paths.get("via").resolve("via-config.yml").toFile());
    private final MCProxyViaBackwardsPlatform backwardsPlatform = new MCProxyViaBackwardsPlatform();
    private java.util.logging.Logger logger = new LoggerWrapper(LoggerFactory.getLogger("ViaVersion"));
    private ClientSession client;

    public MCProxyViaServerProxy(final ClientSession client) {
        this.client = client;
    }

    public void init() {
        config.reload();
        try {
            Via.init(ViaManagerImpl.builder()
                    .platform(this)
                    .commandHandler(new MCProxyViaCommandHandler())
                    .loader(new MCProxyViaLoader())
                    .injector(new MCProxyViaInjector())
                    .build());
        } catch (final Exception e) {
            // fall through
        }
        try {
            backwardsPlatform.initViaBackwards();
        } catch (final Exception e) {
            // fall through
        }
        try {
            ((ViaManagerImpl) Via.getManager()).init();
        } catch (final Exception e) {
            // fall through
        }

    }

    public ChannelInitializer<Channel> inject(final ChannelInitializer<Channel> original) {
        return new MCProxyViaChannelInitializer(original, this.client);
    }

    @Override
    public ProtocolDetectorService protocolDetectorService() {
        return protocolDetectorService;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public String getPlatformName() {
        return "mc-proxy";
    }

    @Override
    public String getPlatformVersion() {
        return "1.0";
    }

    @Override
    public String getPluginVersion() {
        return VersionInfo.VERSION;
    }

    @Override
    public PlatformTask runAsync(Runnable runnable) {
        return new MCProxyPlatformTask(
                SCHEDULED_EXECUTOR_SERVICE.submit(runnable)
        );
    }

    @Override
    public PlatformTask runRepeatingAsync(Runnable runnable, long ticks) {
        return new MCProxyPlatformTask(
                SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(runnable, 0, ticks * 50, MILLISECONDS)
        );
    }

    @Override
    public PlatformTask runSync(Runnable runnable) {
        return runAsync(runnable);
    }

    @Override
    public PlatformTask runSync(Runnable runnable, long ticks) {
        return new MCProxyPlatformTask(
                SCHEDULED_EXECUTOR_SERVICE.schedule(runnable, ticks * 50, MILLISECONDS)
        );
    }

    @Override
    public PlatformTask runRepeatingSync(Runnable runnable, long period) {
        return runRepeatingAsync(runnable, period);
    }

    @Override
    public ViaCommandSender[] getOnlinePlayers() {
        // seems to be only used for commands
        return new ViaCommandSender[0];
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        // seems to be only used for commands
    }

    @Override
    public boolean kickPlayer(UUID uuid, String message) {
        // only going to have one "player" connected so who cares
        return false;
    }

    @Override
    public boolean isPluginEnabled() {
        // todo: add config option
        return true;
    }

    @Override
    public ViaAPI<MinecraftProtocol> getApi() {
        return api;
    }

    @Override
    public ViaVersionConfig getConf() {
        return config;
    }

    @Override
    public File getDataFolder() {
        return new File("build/via");
    }

    @Override
    public void onReload() {

    }

    @Override
    public JsonObject getDump() {
        // only for a command
        return null;
    }

    @Override
    public boolean isOldClientsAllowed() {
        return true;
    }

    @Override
    public boolean hasPlugin(String name) {
        return false;
    }
}
