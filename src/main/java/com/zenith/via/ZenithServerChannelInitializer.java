package com.zenith.via;

import com.github.steveice10.packetlib.tcp.TcpServer;
import com.github.steveice10.packetlib.tcp.TcpServerChannelInitializer;
import io.netty.channel.Channel;

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
