package com.zenith.via;

import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_15;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.zenith.util.Wait;
import com.zenith.via.handler.ZViaProtocolStateHandler;
import io.netty.channel.Channel;
import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.vialoader.netty.ViaCodec;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Shared.*;

public class ZenithViaInitializer {
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void init() {
        if (this.initialized.compareAndSet(false, true)) {
            ViaLoader.init(
                new ZenithViaPlatform(),
                new ZenithViaLoader(),
                null,
                null,
                () -> {
                    // there's some race condition in via loading here that can cause viabackwards to fail its init
                    // might be related to graalvm compiler optimizations, haven't reproduced on java yet
                    // this code is just adding some retries very hackily
                    if (!Wait.waitUntil(() -> {
                        try {
                            EntityTypes1_15.PUFFERFISH.getId();
                            return true;
                        } catch (final Throwable e) {
                            return false;
                        }
                    }, 5)) {
                        DEFAULT_LOG.error("Timed out waiting for via entity id mappings to load :(");
                    }
                    return new ViaBackwardsPlatformImpl();
                }
            );
        }
    }

    // pipeline order before readTimeout -> encryption -> sizer -> compression -> codec -> manager
    // pipeline order after readTimeout -> encryption -> sizer -> compression -> via-codec -> codec -> manager

    public void clientViaChannelInitializer(Channel channel, Session client) {
        if (!CONFIG.client.viaversion.enabled) return;
        if (CONFIG.client.viaversion.autoProtocolVersion) updateClientViaProtocolVersion();
        if (CONFIG.client.viaversion.protocolVersion == MinecraftCodec.CODEC.getProtocolVersion()) {
            CLIENT_LOG.warn("ViaVersion enabled but the protocol is the same as ours, connecting without ViaVersion");
        }
        // todo: add back when 2b2t adds 1.21 via support
//        else if (Proxy.getInstance().isOn2b2t()) {
//            CLIENT_LOG.warn("ViaVersion enabled but server set to 2b2t.org, connecting without ViaVersion");
//        }
        else {
            init();
            UserConnectionImpl userConnection = new UserConnectionImpl(channel, true);
            new ProtocolPipelineImpl(userConnection);
            channel.pipeline().addBefore("codec", "via-protocol-state", new ZViaProtocolStateHandler(client));
            channel.pipeline().addBefore("via-protocol-state", VLPipeline.VIA_CODEC_NAME, new ViaCodec(userConnection));
        }
    }

    public void serverViaChannelInitializer(final Channel channel) {
        if (!CONFIG.server.viaversion.enabled) return;
        init();
        var userConnection = new UserConnectionImpl(channel, false);
        new ProtocolPipelineImpl(userConnection);
        channel.pipeline().addBefore("codec", VLPipeline.VIA_CODEC_NAME, new ViaCodec(userConnection));
    }

    private void updateClientViaProtocolVersion() {
        try {
            final int detectedVersion = ProtocolVersionDetector.getProtocolVersion(
                CONFIG.client.server.address,
                CONFIG.client.server.port);
            if (!ProtocolVersion.isRegistered(detectedVersion)) {
                CLIENT_LOG.error(
                    "Unknown protocol version {} detected for server: {}:{}",
                    detectedVersion,
                    CONFIG.client.server.address,
                    CONFIG.client.server.port);
                return;
            }
            CLIENT_LOG.info(
                "Updating detected protocol version {} for server: {}:{}",
                detectedVersion,
                CONFIG.client.server.address,
                CONFIG.client.server.port);
            CONFIG.client.viaversion.protocolVersion = detectedVersion;
            saveConfigAsync();
        } catch (final Exception e) {
            CLIENT_LOG.error(
                "Failed to detect protocol version for server: {}:{}",
                CONFIG.client.server.address,
                CONFIG.client.server.port,
                e);
        }
    }
}
