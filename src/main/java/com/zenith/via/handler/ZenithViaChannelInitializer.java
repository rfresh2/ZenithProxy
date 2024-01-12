package com.zenith.via.handler;

import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.NonNull;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.vialoader.netty.ViaCodec;

import java.lang.reflect.Method;

public class ZenithViaChannelInitializer extends ChannelInitializer<Channel> {
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

    public ZenithViaChannelInitializer(ChannelInitializer<Channel> original) {
        this.original = original;
    }

    @Override
    protected void initChannel(@NonNull Channel channel) throws Exception {
        INIT_CHANNEL.invoke(original, channel);
        UserConnectionImpl userConnection = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(userConnection);
        // pipeline order before readTimeout -> encryption -> sizer -> compression -> codec -> manager
        // pipeline order after readTimeout -> encryption -> sizer -> compression -> via-codec -> codec -> manager
        channel.pipeline().addBefore("codec", VLPipeline.VIA_CODEC_NAME, new ViaCodec(userConnection));
    }
}
