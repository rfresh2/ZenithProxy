package com.zenith.via.handler;

import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.zenith.network.client.ClientSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.NonNull;

import java.lang.reflect.Method;

public class MCProxyViaChannelInitializer extends ChannelInitializer<Channel> {
    private static final Method INIT_CHANNEL;

    static {
        try {
            INIT_CHANNEL = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            INIT_CHANNEL.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ChannelInitializer<Channel> original;
    private final ClientSession client;

    public MCProxyViaChannelInitializer(ChannelInitializer<Channel> original, final ClientSession client) {
        this.original = original;
        this.client = client;
    }

    @Override
    protected void initChannel(@NonNull Channel channel) throws Exception {
        INIT_CHANNEL.invoke(original, channel);
        UserConnectionImpl userConnection = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(userConnection);
        // outbound order before: manager -> codec -> compression -> sizer -> encryption -> readTimeout
        // outbound order after: manager -> via-encoder -> codec -> compression -> sizer -> encryption -> readTimeout

        // inbound order before: readTimeout -> encryption -> sizer -> compression -> codec -> manager
        // inbound order after: readTimeout -> encryption -> sizer -> compression -> via-decoder -> codec -> manager

        // pipeline order before readTimeout -> encryption -> sizer -> compression -> codec -> manager
        // pipeline order after readTimeout -> encryption -> sizer -> compression -> via-encoder -> via-decoder -> codec -> manager
        channel.pipeline().addBefore("codec", "via-encoder", new MCProxyViaEncodeHandler(userConnection, this.client));
        channel.pipeline().addBefore("codec", "via-decoder", new MCProxyViaDecodeHandler(userConnection, this.client));
    }
}
