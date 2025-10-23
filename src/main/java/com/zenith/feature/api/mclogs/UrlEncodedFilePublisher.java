package com.zenith.feature.api.mclogs;

import lombok.Data;

import java.io.BufferedReader;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

@Data
public class UrlEncodedFilePublisher implements HttpRequest.BodyPublisher {
    private final Path file;
    private final Charset charset;
    private final String formField;

    @Override
    public long contentLength() {
        // unknown (HttpClient will use chunked transfer encoding)
        return -1;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        SubmissionPublisher<ByteBuffer> publisher = new SubmissionPublisher<>();
        publisher.subscribe(subscriber);

        Thread.startVirtualThread(() -> processPublisher(publisher));
    }

    private void processPublisher(final SubmissionPublisher<ByteBuffer> publisher) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // send form key first
            publisher.submit(ByteBuffer.wrap((formField + "=").getBytes(StandardCharsets.UTF_8)));

            char[] buf = new char[4096];
            StringBuilder chunk = new StringBuilder();

            int n;
            while ((n = reader.read(buf)) != -1) {
                chunk.append(buf, 0, n);

                // URL-encode this chunk and send
                String encoded = URLEncoder.encode(chunk.toString(), charset);
                publisher.submit(ByteBuffer.wrap(encoded.getBytes(StandardCharsets.UTF_8)));

                chunk.setLength(0);
            }

            publisher.close();
        } catch (Exception e) {
            publisher.closeExceptionally(e);
        }
    }
}

