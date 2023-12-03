package com.zenith.util;

import com.google.common.base.Suppliers;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static com.zenith.Shared.DEFAULT_LOG;

public final class RefStrings {
    private static final String BRAND = "ZenithProxy";

    public static Supplier<byte[]> BRAND_SUPPLIER = Suppliers.memoize(() -> {
        final ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(byteOutStream)) {
            out.writeUTF(BRAND);
        } catch (IOException e) {
            DEFAULT_LOG.error("Failed to write brand to byte array output stream!", e);
        }
        return byteOutStream.toByteArray();
    });
}
