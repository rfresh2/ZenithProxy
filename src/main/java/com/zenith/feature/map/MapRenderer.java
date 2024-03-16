package com.zenith.feature.map;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.zenith.Shared.DEFAULT_LOG;
import static com.zenith.Shared.MAP_BLOCK_COLOR_MANAGER;

public class MapRenderer {
    private static final Path mapsOutputPath = Path.of("maps");

    public static byte[] render(final byte[] mapData, final int mapId) {
        return render(mapData, mapId, 128);
    }

    public static byte[] render(final byte[] mapData, final int mapId, final int size) {
        if (!mapsOutputPath.toFile().exists()) {
            mapsOutputPath.toFile().mkdir();
        }

        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        var info = new ImageInfo(size, size, 8, false);
        final PngWriter png = new PngWriter(byteStream, info);

        // render map
        for (int i = 0; i < size; i++) {
            final ImageLineInt line = new ImageLineInt(info);
            for (int j = 0; j < size; j++) {
                int k = j + i * size;
                if (k >= mapData.length) {
                    DEFAULT_LOG.error("Failed to render pixel: {} {}", i, j);
                    break;
                }
                int colorFromPackedId = getColorFromPackedId(mapData[k]);
                ImageLineHelper.setPixelRGB8(line, j, colorFromPackedId);
            }
            png.writeRow(line);
        }
        png.end();
        final byte[] bytes = byteStream.toByteArray();

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        final String isoDate = formatter.format(ZonedDateTime.now());
        final File outputfile = mapsOutputPath.resolve(isoDate + "_map_" + mapId + ".png").toFile();
        try {
            Files.write(outputfile.toPath(), bytes);
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to write map image", e);
        }
        return bytes;
    }

    public static int getColorFromPackedId(int i) {
        int j = i & 0xFF;
        int colorId = j >> 2;
        return MAP_BLOCK_COLOR_MANAGER.calculateRGBColorI(
            MAP_BLOCK_COLOR_MANAGER.getColor(colorId),
            Brightness.byId(j & 3));
    }
}
