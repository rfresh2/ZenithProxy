package net.daporkchop.toobeetooteebot.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class RefStrings {
    public static final String BRAND = "Pork2b2tBot_0.1a";
    public static final byte[] BRAND_ENCODED;

    static {
        ByteBuf buf = Unpooled.buffer(5 + BRAND.length());
        try {
            writeUTF8(buf, BRAND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BRAND_ENCODED = buf.array();
    }

    /**
     * Writes an UTF8 string to a byte buffer.
     *
     * @param buf   The byte buffer to write too
     * @param value The string to write
     * @throws java.io.IOException If the writing fails
     */
    public static void writeUTF8(ByteBuf buf, String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= Short.MAX_VALUE) {
            throw new IOException("Attempt to write a string with a length greater than Short.MAX_VALUE to ByteBuf!");
        }
        // Write the string's length
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * Writes an integer into the byte buffer using the least possible amount of bits.
     *
     * @param buf   The byte buffer to write too
     * @param value The integer value to write
     */
    public static void writeVarInt(ByteBuf buf, int value) {
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
