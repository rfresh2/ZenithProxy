package com.zenith.util;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Queue {
    private static final String apiUrl = "https://2bqueue.info/players";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static QueueStatus getQueueStatus() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();
            return mapper.readValue(responseStream, QueueStatus.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
