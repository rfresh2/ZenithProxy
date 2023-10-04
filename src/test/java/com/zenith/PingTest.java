package com.zenith;

import com.zenith.via.ProtocolVersionDetector;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PingTest {

//    @Test
    public void constPing() {
        // testing ping srv dns resolver
        assertEquals(ProtocolVersionDetector.getProtocolVersion("constantiam.net", 25565), 762);
    }

//    @Test
    public void nineb9tPing() {
        // testing ping srv dns resolver
        assertEquals(ProtocolVersionDetector.getProtocolVersion("9b9t.com", 25565), 757);
    }

//    @Test
    public void twob2tPing() {
        // testing ping srv dns resolver
        assertEquals(ProtocolVersionDetector.getProtocolVersion("2b2t.org", 25565), 763);
    }
}
