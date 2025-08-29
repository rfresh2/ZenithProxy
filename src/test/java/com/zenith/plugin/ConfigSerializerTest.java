package com.zenith.plugin;

import com.zenith.plugin.api.ConfigSerializer;
import kotlin.Metadata;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigSerializerTest {

    @Test
    public void testDefaultGsonConfigSerializer() throws Exception {
        readWriteTest(DefaultGsonConfigSerializer.INSTANCE);
    }

    @Test
    public void testKotlinObjectGsonConfigSerializer() throws Exception {
        readWriteTest(new KotlinObjectGsonConfigSerializer(TestConfigKotlinObject.class));
    }

    private void readWriteTest(ConfigSerializer serializer) throws Exception {
        var testConfig = new TestConfig();
        var tempFile = File.createTempFile("testConfig", ".json");
        tempFile.deleteOnExit();
        try (var writer = new FileWriter(tempFile)) {
            serializer.write(testConfig, writer);
        }
        assertTrue(tempFile.exists());
        try (var reader = new FileReader(tempFile)) {
            var readConfig = serializer.read(TestConfig.class, reader);
            assertEquals(testConfig, readConfig);
        }
    }

    @Metadata
    @EqualsAndHashCode
    public static class TestConfigKotlinObject {
        public static final TestConfigKotlinObject INSTANCE = new TestConfigKotlinObject();
        public String testField = "test123";
    }

    @EqualsAndHashCode
    public static class TestConfig {
        public String testField = "test123";
    }
}
