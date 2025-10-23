package com.zenith.feature.api.mclogs;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mclogs.model.MclogsResponse;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.zenith.Globals.DEFAULT_LOG;
import static com.zenith.Globals.OBJECT_MAPPER;

public class MclogsApi extends Api {
    public static final MclogsApi INSTANCE = new MclogsApi();
    public MclogsApi() {
        super("https://api.mclo.gs");
    }

    public Optional<MclogsResponse> uploadLog(Path logFilePath) {
        try {
            if (!Files.exists(logFilePath) || !Files.isRegularFile(logFilePath)) {
                DEFAULT_LOG.error("Log file does not exist or is not a regular file: {}", logFilePath);
                return Optional.empty();
            }
            // todo: test if needed
//            try {
//                // tail the log file if above 10MB
//                // careful to not cause OOM, need to handle this as a stream
//                var size = Files.size(logFilePath);
//                var tenMB = 10 * 1024 * 1024;
//                if (size > tenMB) {
//                    var tempFilePath = Files.createTempFile("mclogs_tail_", ".log");
//                    var tempFile = tempFilePath.toFile();
//                    DEFAULT_LOG.warn("Log is larger than 10MB, tailing the last 10MB to temp file: {}", tempFilePath);
//                    tempFile.deleteOnExit();
//                    try (
//                        var raf = new RandomAccessFile(logFilePath.toFile(), "r");
//                        var writer = Files.newBufferedWriter(tempFilePath, StandardCharsets.UTF_8)
//                    ) {
//                        // may cut off in the middle of a utf-8 character, not sure if this will work
//                        long pointer = Math.max(0, size - tenMB);
//                        raf.seek(pointer);
//                        String line;
//                        while ((line = raf.readLine()) != null) {
//                            writer.write(line);
//                            writer.newLine();
//                        }
//                    }
//                    logFilePath = tempFilePath;
//                }
//            } catch (Exception e) {
//                DEFAULT_LOG.error("Failed tailing log file", e);
//            }

            var req = buildBaseRequest("/1/log")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(new UrlEncodedFilePublisher(logFilePath, StandardCharsets.UTF_8, "content"))
                .build();
            try (var client = buildHttpClient()) {
                var response = client.send(req, HttpResponse.BodyHandlers.ofString());
                var body = response.body();
                return Optional.of(OBJECT_MAPPER.readValue(body, MclogsResponse.class));
            } catch (Exception e) {
                DEFAULT_LOG.error("Failed parsing MCLogs response", e);
                return Optional.empty();
            }
        } catch (Exception e) {
            DEFAULT_LOG.error("Failed upload log to mclogs", e);
            return Optional.empty();
        }
    }
}
