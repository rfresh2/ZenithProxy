package com.zenith.via;

import io.netty.channel.Channel;
import org.geysermc.mcprotocollib.network.tcp.TcpServer;
import org.geysermc.mcprotocollib.network.tcp.TcpServerChannelInitializer;

import static com.zenith.Shared.VIA_INITIALIZER;

public class ZenithServerChannelInitializer extends TcpServerChannelInitializer {
    public static Factory FACTORY = ZenithServerChannelInitializer::new;

    public ZenithServerChannelInitializer(final TcpServer server) {
        super(server);
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception {
        super.initChannel(channel);
        VIA_INITIALIZER.serverViaChannelInitializer(channel);
    }
}
