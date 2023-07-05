package com.zenith.mcping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.zenith.mcping.LegacyPinger.LegacyPingResponse;
import com.zenith.mcping.data.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static com.zenith.Shared.CLIENT_LOG;

public class MCPing {
    /**
     * If the client is pinging to determine what version to use, by convention -1 should be set.
     */
    public static final int PROTOCOL_VERSION_DISCOVERY = -1;
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public ResponseDetails getLegacyPingWithDetails(PingOptions options) throws IOException {
        LegacyPinger pinger = new LegacyPinger();
        pinger.setProtocolVersion(options.getProtocolVersion());
        pinger.setAddress(new InetSocketAddress(options.getHostname(), options.getPort()));
        pinger.setTimeout(options.getTimeout());
        pinger.setProtocolVersion(options.getProtocolVersion());
        LegacyPingResponse response = pinger.ping();
        return new ResponseDetails(response.toFinalResponse(), null, null, null, null, null, null, response, null);
    }

    public ResponseDetails getPingWithDetails(PingOptions options) throws IOException {
        Pinger a = new Pinger();
        a.setAddress(new InetSocketAddress(options.getHostname(), options.getPort()));
        a.setTimeout(options.getTimeout());
        a.setProtocolVersion(options.getProtocolVersion());
        String json = a.fetchData();
        try {
            if (json != null) {
                if (json.getBytes(StandardCharsets.UTF_8).length > 5000000) {
                    // got a response greater than 5mb, possible honeypot?
                    return null;
                }
                if (json.contains("{")) {
                    if (json.contains("\"modid\"") && json.contains("\"translate\"")) { //it's a forge response translate
                        ForgeResponseTranslate forgeResponseTranslate = GSON.fromJson(json, ForgeResponseTranslate.class);
                        return new ResponseDetails(forgeResponseTranslate.toFinalResponse(), forgeResponseTranslate, null, null, null, null, null, null, json);
                    } else if (json.contains("\"modid\"") && json.contains("\"text\"")) { //it's a normal forge response
                        ForgeResponse forgeResponse = GSON.fromJson(json, ForgeResponse.class);
                        return new ResponseDetails(forgeResponse.toFinalResponse(), null, forgeResponse, null, null, null, null, null, json);
                    } else if (json.contains("\"modid\"")) {  //it's an old forge response
                        ForgeResponseOld forgeResponseOld = GSON.fromJson(json, ForgeResponseOld.class);
                        return new ResponseDetails(forgeResponseOld.toFinalResponse(), null, null, forgeResponseOld, null, null, null, null, json);
                    } else if (json.contains("\"extra\"")) { //it's an extra response
                        ExtraResponse extraResponse = GSON.fromJson(json, ExtraResponse.class);
                        return new ResponseDetails(extraResponse.toFinalResponse(), null, null, null, extraResponse, null, null, null, json);
                    } else if (json.contains("\"text\"")) { //it's a new response
                        NewResponse newResponse = GSON.fromJson(json, NewResponse.class);
                        return new ResponseDetails(newResponse.toFinalResponse(), null, null, null, null, newResponse, null, null, json);
                    } else { //it's an old response
                        OldResponse oldResponse = GSON.fromJson(json, OldResponse.class);
                        return new ResponseDetails(oldResponse.toFinalResponse(), null, null, null, null, null, oldResponse, null, json);
                    }
                }
            }
        } catch (Throwable e) {
            CLIENT_LOG.error("Failed to ping " + options, e);
            if (e instanceof JsonSyntaxException) {
                throw e;
            }
        }
        return null;
    }

    public static class ResponseDetails {
        public final FinalResponse standard;
        public final ForgeResponseTranslate forgeTranslate;
        public final ForgeResponse forge;
        public final ForgeResponseOld oldForge;
        public final ExtraResponse extraResponse;
        public final NewResponse response;
        public final OldResponse oldResponse;
        public final LegacyPingResponse legacyResponse;
        public final String json;

        public ResponseDetails(FinalResponse standard,
                               ForgeResponseTranslate forgeTranslate,
                               ForgeResponse forge,
                               ForgeResponseOld oldForge,
                               ExtraResponse extraResponse,
                               NewResponse response,
                               OldResponse oldResponse,
                               LegacyPingResponse legacyResponse,
                               String json) {
            this.standard = standard;
            this.forgeTranslate = forgeTranslate;
            this.forge = forge;
            this.oldForge = oldForge;
            this.extraResponse = extraResponse;
            this.response = response;
            this.oldResponse = oldResponse;
            this.legacyResponse = legacyResponse;
            this.json = json;
        }

//        public static void main(String[] args) throws IOException {
//            MCPing pinger = new MCPing();
////            Integer protocol = ProtocolVersions.findProtocolVersion("1.17").get();
//            Integer protocol = -1;
//            PingOptions options = new PingOptions();
//            options.setHostname("51.81.154.27");
//            options.setPort(25565);
//            options.setTimeout(5000);
//            options.setProtocolVersion(protocol);
//            try {
//                ResponseDetails pingWithDetails = pinger.getPingWithDetails(options);
//                System.out.println(pingWithDetails.standard.getVersion());
//                System.out.println(pingWithDetails.standard.getVersion().getProtocol());
//                ProtocolVersions.findVersionForProtocol(pingWithDetails.standard.getVersion().getProtocol()).ifPresent(s -> {
//                    System.out.println(s);
//                });
//            } catch (Throwable e) {
//            }
//        }
    }
}
