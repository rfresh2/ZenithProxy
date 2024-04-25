package com.zenith.generator;

import com.zenith.DataGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Language implements Generator {
    @Override
    public void generate() {
        try {
            byte[] bytes = Language.class.getResourceAsStream("/assets/minecraft/lang/en_us.json").readAllBytes();
            Files.write(
                DataGenerator.outputFile("language.json").toPath(),
                bytes,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
            DataGenerator.LOG.info("Dumped language.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
