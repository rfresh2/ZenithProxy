package com.zenith.util;

import com.zenith.Shared;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import static com.zenith.Shared.DEFAULT_LOG;
import static com.zenith.util.DisconnectReasonInfo.DisconnectCategory.*;

@UtilityClass
public class DisconnectReasonInfo {

    @Getter
    public enum DisconnectCategory {
        KICK("kicked"),
        CONNECTION_ISSUE_PLAYER("connection-issue-you"),
        CONNECTION_ISSUE_2B2T("connection-issue-2b2t"),
        CONNECTION_ISSUE("connection-issue"),
        MANUAL("manual-disconnect"),
        ZENITH_MODULE("zenithproxy-modules"),
        SERVER_RESTART("server-restart"),
        SOCKS_PROXY("socks5-proxy"),
        AUTHENTICATION_FAIL("authentication-failure"),
        AUTHENTICATION_RATE_LIMIT("authentication-rate-limiting"),
        ALREADY_CONNECTED("already-connected"),
        ILLEGAL_DISCONNECT("illegal-disconnect");

        private final String wikiHeader;

        DisconnectCategory(String wikiHeader) {
            this.wikiHeader = wikiHeader;
        }

        public String getWikiURL() {
            return "https://github.com/rfresh2/ZenithProxy/wiki/Disconnects#" + wikiHeader;
        }
    }

    public DisconnectCategory getDisconnectCategory(final String reason) {
        if (reason.startsWith("You have lost connection to the server")) {
            return KICK;
        } else if (reason.startsWith("Read timed out.")) {
            return CONNECTION_ISSUE;
        } else if (reason.startsWith("An internal error occurred in your connection")) {
            return CONNECTION_ISSUE_2B2T;
        } else if (reason.startsWith(Shared.LOGIN_FAILED)) {
            return AUTHENTICATION_FAIL;
        } else if (reason.equals(Shared.MANUAL_DISCONNECT)) {
            return MANUAL;
        } else if (reason.equals(Shared.AUTO_DISCONNECT)) {
            return ZENITH_MODULE;
        } else if (reason.equals(Shared.SYSTEM_DISCONNECT)) {
            return ZENITH_MODULE;
        } else if (reason.equals(Shared.SERVER_RESTARTING)) {
            return SERVER_RESTART;
        } else if (reason.startsWith("io.netty.handler.proxy.ProxyConnectException")) {
            return SOCKS_PROXY;
        } else if (reason.startsWith("Authentication servers are down")) {
            return AUTHENTICATION_RATE_LIMIT;
        } else if (reason.startsWith("Your connection to 2b2t encountered a problem")) {
            return CONNECTION_ISSUE_2B2T;
        } else if (reason.startsWith("Connection closed")) {
            return CONNECTION_ISSUE;
        } else if (reason.startsWith("Server closed")) {
            return SERVER_RESTART;
        } else if (reason.startsWith("You are logging in too fast")) {
            return AUTHENTICATION_RATE_LIMIT;
        } else if (reason.startsWith("Connection timed out")) {
            return CONNECTION_ISSUE_PLAYER;
        } else if (reason.startsWith("Proxy shutting down")) {
            return SERVER_RESTART;
        } else if (reason.startsWith("You are already connected to this proxy")) {
            return ALREADY_CONNECTED;
        } else if (reason.startsWith("Illegal characters in chat")) {
            return ILLEGAL_DISCONNECT;
        }
        DEFAULT_LOG.debug("Unknown disconnect reason category for: {}", reason);
        return CONNECTION_ISSUE;
    }

}
