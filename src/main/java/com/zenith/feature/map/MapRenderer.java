package com.zenith.feature.map;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
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
    static {
        if (!mapsOutputPath.toFile().exists()) {
            mapsOutputPath.toFile().mkdir();
        }
    }

    public static byte[] render(final byte[] mapData, final int mapId) {
        return render(mapData, mapId, 128);
    }

    public static byte[] render(final byte[] mapData, final int mapId, final int size) {
        // render map
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int k = j + i * size;
                if (k >= mapData.length) {
                    DEFAULT_LOG.error("Failed to render pixel: {} {}", i, j);
                    break;
                }
                int colorFromPackedId = getColorFromPackedId(mapData[k]);
                image.setRGB(j, i, colorFromPackedId);
            }
        }

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        final String isoDate = formatter.format(ZonedDateTime.now());
        File outputfile = mapsOutputPath.resolve(isoDate + "_map_" + mapId + ".png").toFile();
        var byteStream = new ByteArrayOutputStream();
        try (var outputStream = new BufferedOutputStream(byteStream)) {
            ImageIO.write(image, "png", outputStream);
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed to write map image", e);
        }
        var bytes = byteStream.toByteArray();

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
        return MAP_BLOCK_COLOR_MANAGER.calculateRGBColor(
            MAP_BLOCK_COLOR_MANAGER.getColor(colorId),
            Brightness.byId(j & 3))
            .getRGB();
    }
}
