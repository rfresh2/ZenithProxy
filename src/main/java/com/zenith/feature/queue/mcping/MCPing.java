package com.zenith.feature.queue.mcping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zenith.feature.queue.mcping.data.ExtraResponse;
import com.zenith.feature.queue.mcping.data.FinalResponse;
import com.zenith.feature.queue.mcping.rawData.*;
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
import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.OBJECT_MAPPER;

public class MCPing {
    /**
     * If the client is pinging to determine what version to use, by convention -1 should be set.
     */
    public static final int PROTOCOL_VERSION_DISCOVERY = -1;
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";
    private static final EventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setNameFormat("MCPing-%d").build());

    public int getProtocolVersion(String hostname, int port, int timeout, boolean resolveDns) throws IOException {
        final InetSocketAddress address;
        if (resolveDns) address = resolveAddress(hostname, port);
        else address = new InetSocketAddress(hostname, port);
        var json = Pinger.fetchData(address, timeout, PROTOCOL_VERSION_DISCOVERY);
        var jsonTree = OBJECT_MAPPER.readTree(json);
        var versionNode = jsonTree.get("version");
        return versionNode.get("protocol").asInt();
    }

    public FinalResponse ping(String hostname, int port, int timeout, boolean resolveDns) throws IOException {
        final InetSocketAddress address;
        if (resolveDns) address = resolveAddress(hostname, port);
        else address = new InetSocketAddress(hostname, port);
        var json = Pinger.fetchData(address, timeout, PROTOCOL_VERSION_DISCOVERY);
        return parsePing(json);
    }

    private InetSocketAddress resolveAddress(final String hostname, final int defaultPort) {
        final String srvRecord = "_minecraft._tcp." + hostname;
        String resolvedHostname = hostname;
        int resolvedPort = defaultPort;
        if(!hostname.matches(IP_REGEX) && !hostname.equalsIgnoreCase("localhost")) {
            AddressedEnvelope<DnsResponse, InetSocketAddress> envelope = null;
            try (DnsNameResolver resolver = new DnsNameResolverBuilder(EVENT_LOOP_GROUP.next())
                .datagramChannelType(NioDatagramChannel.class)
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

    public FinalResponse parsePing(final String json) {
        try {
            var jsonTree = OBJECT_MAPPER.readTree(json);
            var versionNode = jsonTree.get("version");
            var protocol = versionNode.get("protocol").asInt();
            var versionName = versionNode.get("name").asText();
            var version = new Version();
            version.setName(versionName);
            version.setProtocol(protocol);
            var playersNode = jsonTree.get("players");
            var online = playersNode.get("online").asInt();
            var max = playersNode.get("max").asInt();
            var sampleNode = (ArrayNode) (playersNode.get("sample"));
            var playersList = new ArrayList<Player>();
            sampleNode.forEach(playerListNode -> {
                var player = new Player();
                JsonNode nameNode = playerListNode.get("name");
                if (nameNode != null && nameNode.isTextual())
                    player.setName(nameNode.asText());
                JsonNode idNode = playerListNode.get("id");
                if (idNode != null && idNode.isTextual())
                    player.setId(idNode.asText());
                playersList.add(player);
            });
            var players = new Players();
            players.setMax(max);
            players.setOnline(online);
            players.setSample(playersList);

            var descriptionNode = jsonTree.get("description");
            var descriptionExtraNode = descriptionNode.withArrayProperty("extra");
            final List<Extra> extras = new ArrayList<>();
            descriptionExtraNode.forEach(node -> {
                if (node.isObject()) {
                    var extra = new Extra();
                    JsonNode colorNode = node.get("color");
                    if (colorNode != null && colorNode.isTextual())
                        extra.setColor(colorNode.asText());
                    JsonNode boldNode = node.get("bold");
                    if (boldNode != null && boldNode.isBoolean())
                        extra.setBold(boldNode.asBoolean());
                    JsonNode textNode = node.get("text");
                    if (textNode != null && textNode.isTextual())
                        extra.setText(textNode.asText());
                    extras.add(extra);
                }
            });
            var description = new ExtraDescription();
            description.setText(descriptionNode.get("text").asText());
            description.setExtra(extras.toArray(new Extra[0]));
            var favicon = jsonTree.get("favicon").asText();
            var extraResponse = new ExtraResponse();
            extraResponse.setVersion(version);
            extraResponse.setPlayers(players);
            extraResponse.setDescription(description);
            extraResponse.setFavicon(favicon);
            return extraResponse.toFinalResponse();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
