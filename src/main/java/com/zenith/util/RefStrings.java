package com.zenith.util;

import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.google.common.base.Suppliers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.function.Supplier;

public final class RefStrings {
    private static final String BRAND = "ZenithProxy";

    public static Supplier<byte[]> BRAND_SUPPLIER = Suppliers.memoize(() -> {
        final ByteBuf byteBuf = Unpooled.buffer();
        var minecraftCodecHelper = MinecraftCodec.CODEC.getHelperFactory().get();
        minecraftCodecHelper.writeString(byteBuf, BRAND);
        final byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        byteBuf.release();
        return bytes;
    });
}
