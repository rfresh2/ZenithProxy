package com.zenith.feature.api;

import com.zenith.feature.api.textures.TexturesApi;
import org.junit.jupiter.api.Assertions;

import java.util.Optional;

public class TexturesApiTest {

//    @Test
    public void testTextureDownload() throws Exception {
        String textureUrl = "http://textures.minecraft.net/texture/d3ac53cd2a9f05fd32f8e6c1292fbe96b4af264f8bc3e4bb862e330ee50f34a2";
        Optional<byte[]> texture = TexturesApi.INSTANCE.getTexture(textureUrl);
        Assertions.assertTrue(texture.isPresent(), "Texture should be present");
        Assertions.assertTrue(texture.get().length > 0, "Texture data should not be empty");
        byte[] expectedSkin = readSkinPng();
        Assertions.assertArrayEquals(expectedSkin, texture.get(), "Downloaded texture should match expected skin data");
    }

    private byte[] readSkinPng() throws Exception {
        try (var inStream = getClass().getClassLoader().getResourceAsStream("rfresh2.png")) {
            return inStream.readAllBytes();
        }
    }
}
