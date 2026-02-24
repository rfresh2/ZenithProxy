package com.zenith.feature.skin;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class SkinRendererTest {

    @Test
    public void testRenderHead() throws Exception {
        byte[] skinData = readSkinPng();
        byte[] headData = SkinRenderer.renderHead(skinData, 8);
        Assertions.assertNotNull(headData);
        byte[] expectedData = readHelmPng();
        comparePngs(expectedData, headData);
    }

    private byte[] readSkinPng() throws Exception {
        try (var inStream = getClass().getClassLoader().getResourceAsStream("rfresh2.png")) {
            return inStream.readAllBytes();
        }
    }

    private byte[] readHelmPng() throws  Exception {
        try (var inStream = getClass().getClassLoader().getResourceAsStream("rfresh2-helm.png")) {
            return inStream.readAllBytes();
        }
    }

    private void comparePngs(byte[] expected, byte[] actual) {
        PngReader pngExpected = new PngReader(new ByteArrayInputStream(expected));
        PngReader pngActual = new PngReader(new ByteArrayInputStream(actual));
        Assertions.assertEquals(pngExpected.imgInfo.rows, pngActual.imgInfo.rows);
        Assertions.assertEquals(pngExpected.imgInfo.cols, pngActual.imgInfo.cols);
        for (int i = 0; i < pngExpected.imgInfo.rows; i++) {
            IImageLine expectedLine = pngExpected.readRow(i);
            IImageLine actualLine = pngActual.readRow(i);
            for (int j = 0; j < pngExpected.imgInfo.cols; j++) {
                int expectedPixel = ImageLineHelper.getPixelARGB8(expectedLine, j);
                int actualPixel = ImageLineHelper.getPixelARGB8(actualLine, j);
                Assertions.assertEquals(expectedPixel, actualPixel, "Pixel mismatch at row " + i + ", col " + j);
            }
        }
        pngExpected.close();
        pngActual.close();
    }
}
