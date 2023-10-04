package com.zenith.feature.queue.mcping;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.zenith.feature.queue.mcping.data.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static com.zenith.Shared.CLIENT_LOG;

public class MCPing {
    /**
     * If the client is pinging to determine what version to use, by convention -1 should be set.
     */
    public static final int PROTOCOL_VERSION_DISCOVERY = -1;
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";
    private static final EventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setNameFormat("MCPing-%d").build());
    private static final Class<NioDatagramChannel> DATAGRAM_CHANNEL_CLASS = NioDatagramChannel.class;

    public ResponseDetails getPingWithDetails(PingOptions options) throws IOException {
        Pinger a = new Pinger();
        if (options.isResolveDns()) {
            a.setAddress(resolveAddress(options.getHostname(), options.getPort()));
        } else {
            a.setAddress(new InetSocketAddress(options.getHostname(), options.getPort()));
        }
        a.setTimeout(options.getTimeout());
        a.setProtocolVersion(options.getProtocolVersion());
        String json = a.fetchData();
        try {
            if (json != null) {
                if (json.contains("{")) {
                    if (json.contains("\"modid\"") && json.contains("\"translate\"")) { //it's a forge response translate
                        ForgeResponseTranslate forgeResponseTranslate = GSON.fromJson(json, ForgeResponseTranslate.class);
                        return new ResponseDetails(forgeResponseTranslate.toFinalResponse(), forgeResponseTranslate, null, null, null, null, null, json);
                    } else if (json.contains("\"modid\"") && json.contains("\"text\"")) { //it's a normal forge response
                        ForgeResponse forgeResponse = GSON.fromJson(json, ForgeResponse.class);
                        return new ResponseDetails(forgeResponse.toFinalResponse(), null, forgeResponse, null, null, null, null, json);
                    } else if (json.contains("\"modid\"")) {  //it's an old forge response
                        ForgeResponseOld forgeResponseOld = GSON.fromJson(json, ForgeResponseOld.class);
                        return new ResponseDetails(forgeResponseOld.toFinalResponse(), null, null, forgeResponseOld, null, null, null, json);
                    } else if (json.contains("\"extra\"")) { //it's an extra response
                        ExtraResponse extraResponse = GSON.fromJson(json, ExtraResponse.class);
                        return new ResponseDetails(extraResponse.toFinalResponse(), null, null, null, extraResponse, null, null, json);
                    } else if (json.contains("\"text\"")) { //it's a new response
                        NewResponse newResponse = GSON.fromJson(json, NewResponse.class);
                        return new ResponseDetails(newResponse.toFinalResponse(), null, null, null, null, newResponse, null, json);
                    } else { //it's an old response
                        OldResponse oldResponse = GSON.fromJson(json, OldResponse.class);
                        return new ResponseDetails(oldResponse.toFinalResponse(), null, null, null, null, null, oldResponse, json);
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

    private InetSocketAddress resolveAddress(final String hostname, final int defaultPort) {
        final String srvRecord = "_minecraft._tcp." + hostname;
        String resolvedHostname = hostname;
        int resolvedPort = defaultPort;
        if(!hostname.matches(IP_REGEX) && !hostname.equalsIgnoreCase("localhost")) {
            AddressedEnvelope<DnsResponse, InetSocketAddress> envelope = null;
            try (DnsNameResolver resolver = new DnsNameResolverBuilder(EVENT_LOOP_GROUP.next())
                .channelType(DATAGRAM_CHANNEL_CLASS)
                .build()) {
                envelope = resolver.query(new DefaultDnsQuestion(srvRecord, DnsRecordType.SRV)).get();

                DnsResponse response = envelope.content();
                if (response.count(DnsSection.ANSWER) > 0) {
                    DefaultDnsRawRecord record = response.recordAt(DnsSection.ANSWER, 0);
                    if (record.type() == DnsRecordType.SRV) {
                        ByteBuf buf = record.content();
                        buf.skipBytes(4); // Skip priority and weight.

                        int port = buf.readUnsignedShort();
                        String host = DefaultDnsRecordDecoder.decodeName(buf);
                        if (host.endsWith(".")) {
                            host = host.substring(0, host.length() - 1);
                        }

                        resolvedHostname = host;
                        resolvedPort = port;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (envelope != null) {
                    envelope.release();
                }
            }
        }
        try {
            InetAddress resolved = InetAddress.getByName(resolvedHostname);
            return new InetSocketAddress(resolved, resolvedPort);
        } catch (UnknownHostException e) {
            return InetSocketAddress.createUnresolved(resolvedHostname, resolvedPort);
        }
    }

    public static class ResponseDetails {
        public final FinalResponse standard;
        public final ForgeResponseTranslate forgeTranslate;
        public final ForgeResponse forge;
        public final ForgeResponseOld oldForge;
        public final ExtraResponse extraResponse;
        public final NewResponse response;
        public final OldResponse oldResponse;
        public final String json;

        public ResponseDetails(FinalResponse standard,
                               ForgeResponseTranslate forgeTranslate,
                               ForgeResponse forge,
                               ForgeResponseOld oldForge,
                               ExtraResponse extraResponse,
                               NewResponse response,
                               OldResponse oldResponse,
                               String json) {
            this.standard = standard;
            this.forgeTranslate = forgeTranslate;
            this.forge = forge;
            this.oldForge = oldForge;
            this.extraResponse = extraResponse;
            this.response = response;
            this.oldResponse = oldResponse;
            this.json = json;
        }
    }
}
