package com.zenith.util;

import com.google.common.base.Suppliers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@UtilityClass
public class RefStrings {
    protected static final String BRAND = "ZenithProxy";

    public static Supplier<byte[]> BRAND_SUPPLIER = Suppliers.memoize(() -> {
        ByteBuf buf = Unpooled.buffer(5 + BRAND.length());
        try {
            writeUTF8(buf, BRAND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.array();
    });

    protected void writeUTF8(ByteBuf buf, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= Short.MAX_VALUE) {
            throw new IOException("Attempt to write a string with a length greater than Short.MAX_VALUE to ByteBuf!");
        }
        // Write the string's length
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    protected void writeVarInt(ByteBuf buf, int value) {
        byte part;
        while (true) {
            part = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            buf.writeByte(part);
            if (value == 0) {
                break;
            }
        }
    }
}
