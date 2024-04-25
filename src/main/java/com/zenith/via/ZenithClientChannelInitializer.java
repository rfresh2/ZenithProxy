package com.zenith.via;

import io.netty.channel.Channel;
import org.geysermc.mcprotocollib.network.tcp.TcpClientChannelInitializer;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;

import static com.zenith.Shared.VIA_INITIALIZER;

public class ZenithClientChannelInitializer extends TcpClientChannelInitializer {
    public static Factory FACTORY = ZenithClientChannelInitializer::new;
    private final TcpClientSession client;

    public ZenithClientChannelInitializer(final TcpClientSession client, final boolean isTransferring) {
        super(client, isTransferring);
        this.client = client;
    }

    @Override
    protected void initChannel(final Channel channel) throws Exception {
        super.initChannel(channel);
        VIA_INITIALIZER.clientViaChannelInitializer(channel, this.client);
    }
}
