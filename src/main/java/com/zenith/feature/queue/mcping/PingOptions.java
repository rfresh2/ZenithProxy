package com.zenith.feature.queue.mcping;

public class PingOptions {

    private String hostname;
    private int port;
    private int timeout;
    private int protocolVersion = -1;

    String getHostname() {
        return this.hostname;
    }

    public PingOptions setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    int getPort() {
        return this.port;
    }

    public PingOptions setPort(int port) {
        this.port = port;
        return this;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public PingOptions setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocol) {
        this.protocolVersion = protocol;
    }

    @Override
    public String toString() {
        return hostname + ":" + port;
    }
}
