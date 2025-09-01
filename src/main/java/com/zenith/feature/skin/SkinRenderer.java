package com.zenith.feature.skin;

import ar.com.hjg.pngj.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SkinRenderer {
    private SkinRenderer() {}

    public static byte[] renderHead(final byte[] skinData, int scale) {
        var outBytes = new ByteArrayOutputStream();
        int size = 8 * scale;
        var info = new ImageInfo(size, size, 8, true);
        PngWriter pngw = new PngWriter(outBytes, info);
        int[] baseHead = readHeadPng(skinData);
        int[] baseHat = readHatPng(skinData);

        // write head + hat scaled
        for (int i = 0; i < size; i++) {
            ImageLineInt linew = new ImageLineInt(info);
            for (int j = 0; j < size; j++) {
                int index = ((i / scale) * 8) + (j / scale);
                int pixel = baseHead[index];
                int hatPixel = baseHat[index];
                // if hat pixel is not transparent, use it
                if ((hatPixel >> 24) != 0x00) {
                    pixel = hatPixel;
                }
                ImageLineHelper.setPixelRGBA8(linew, j, pixel);
            }
            pngw.writeRow(linew);
        }
        pngw.end();
        return outBytes.toByteArray();
    }

    // 8x8 head only, no hat
    private static int[] readHeadPng(byte[] skinData) {
        PngReader pngr = new PngReader(new ByteArrayInputStream(skinData));
        int[] baseHead = new int[64];
        for (int i = 8; i < 16; i++) {
            IImageLine liner = pngr.readRow(i);
            for (int j = 8; j < 16; j++) {
                baseHead[((i - 8) * 8) + (j - 8)] = ImageLineHelper.getPixelARGB8(liner, j);
            }
        }
        pngr.close();
        return baseHead;
    }

    // 8x8 hat only
    private static int[] readHatPng(byte[] skinData) {
        PngReader pngr = new PngReader(new ByteArrayInputStream(skinData));
        int[] baseHat = new int[64];
        for (int i = 8; i < 16; i++) {
            IImageLine liner = pngr.readRow(i);
            for (int j = 40; j < 48; j++) {
                baseHat[((i - 8) * 8) + (j - 40)] = ImageLineHelper.getPixelARGB8(liner, j);
            }
        }
        pngr.close();
        return baseHat;
    }

}

