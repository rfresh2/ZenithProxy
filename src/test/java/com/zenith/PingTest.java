package com.zenith;

import com.zenith.via.ProtocolVersionDetector;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingTest {

//    @Test
    public void constPing() {
        // testing ping srv dns resolver
        assertEquals(ProtocolVersionDetector.getProtocolVersion("constantiam.net", 25565), 762);
    }
}
