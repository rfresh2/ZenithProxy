package com.zenith.via;

import com.viaversion.vialoader.ViaLoader;
import com.viaversion.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import com.viaversion.vialoader.impl.platform.ViaRewindPlatformImpl;
import com.viaversion.vialoader.netty.VLPipeline;
import com.viaversion.vialoader.netty.ViaCodec;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.zenith.Proxy;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.geysermc.mcprotocollib.network.tcp.TcpPacketCodec;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Globals.*;

public class ZenithViaInitializer {
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void init() {
        if (this.initialized.compareAndSet(false, true)) {
            ViaLoader.init(
                new ZenithViaPlatform(),
                new ZenithViaLoader(),
                null,
                null,
                ViaBackwardsPlatformImpl::new,
                ViaRewindPlatformImpl::new
            );
        }
    }

    public void clientViaChannelInitializer(Channel channel) {
        if (!CONFIG.client.viaversion.enabled) return;
        if (CONFIG.client.viaversion.autoProtocolVersion) updateClientViaProtocolVersion();
        if (CONFIG.client.viaversion.protocolVersion == MinecraftCodec.CODEC.getProtocolVersion()) {
            CLIENT_LOG.warn("ViaVersion enabled but the protocol is the same as ours, connecting without ViaVersion");
        } else if (CONFIG.client.viaversion.disableOn2b2t && Proxy.getInstance().isOn2b2t()) {
            CLIENT_LOG.warn("ViaVersion enabled but server set to 2b2t.org, connecting without ViaVersion");
        } else {
            init();
            UserConnectionImpl userConnection = new UserConnectionImpl(channel, true);
            new ProtocolPipelineImpl(userConnection);
            channel.pipeline().addBefore(TcpPacketCodec.ID, VLPipeline.VIA_CODEC_NAME, new ViaCodec(userConnection));
            channel.attr(VIA_USER).set(userConnection);
        }
    }

    public static final AttributeKey<UserConnectionImpl> VIA_USER = AttributeKey.newInstance("ViaUser");

    public void serverViaChannelInitializer(final Channel channel) {
        if (!CONFIG.server.viaversion.enabled) return;
        init();
        var userConnection = new UserConnectionImpl(channel, false);
        new ProtocolPipelineImpl(userConnection);
        channel.pipeline().addBefore(TcpPacketCodec.ID, VLPipeline.VIA_CODEC_NAME, new ViaCodec(userConnection));
        channel.attr(VIA_USER).set(userConnection);
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
