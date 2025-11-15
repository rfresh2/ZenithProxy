package com.zenith;

import com.zenith.util.ReplayReader;

import java.io.File;
import java.nio.file.Path;

public class ReplayRecordingReaderTest {

//    @Test
    public void readTestRecording() {
        File replayFile = Path.of("replays", "reader-test.mcpr").toFile();
        File outputFile = replayFile.getParentFile().toPath().resolve("replay.log").toFile();
        final ReplayReader replayReader = new ReplayReader(replayFile, outputFile);
        replayReader.read();
    }
}
