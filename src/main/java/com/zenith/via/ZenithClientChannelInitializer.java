package com.zenith.via;

import com.github.steveice10.packetlib.tcp.TcpClientChannelInitializer;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import io.netty.channel.Channel;

import static com.zenith.Shared.VIA_INITIALIZER;

public class ZenithClientChannelInitializer extends TcpClientChannelInitializer {
    public static Factory FACTORY = ZenithClientChannelInitializer::new;

    public ZenithClientChannelInitializer(final TcpClientSession client) {
        super(client);
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception {
        super.initChannel(channel);
        VIA_INITIALIZER.clientViaChannelInitializer(channel);
    }
}
