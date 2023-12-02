package com.zenith.network.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.UnexpectedEncryptionException;
import com.github.steveice10.mc.protocol.data.handshake.HandshakeIntent;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoHandler;
import com.github.steveice10.mc.protocol.data.status.handler.ServerPingTimeHandler;
import com.github.steveice10.mc.protocol.packet.common.clientbound.*;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.github.steveice10.mc.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundKeyPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import com.github.steveice10.mc.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.data.config.ResourcePack;
import com.zenith.event.proxy.ConnectEvent;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

import static com.zenith.Shared.*;
import static java.util.Objects.isNull;

public class ClientListener implements SessionListener {
    @NonNull ClientSession session;
    private final @NonNull ProtocolState targetState = ProtocolState.LOGIN;

    public ClientListener(final @NotNull ClientSession session) {
        this.session = session;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            defaultPacketReceived(session, packet);
            if (((MinecraftProtocol) session.getPacketProtocol()).getState() != ProtocolState.GAME) return;
            Packet p = CLIENT_HANDLERS.handleInbound(packet, this.session);
            if (p != null) {
                for (ServerConnection connection : Proxy.getInstance().getActiveConnections()) {
                    connection.sendAsync(packet); // sends on each connection's own event loop
                }
            }
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    private void defaultPacketReceived(Session session, Packet packet) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (protocol.getState() == ProtocolState.LOGIN) {
            if (packet instanceof ClientboundHelloPacket helloPacket) {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                String accessToken = session.getFlag(MinecraftConstants.ACCESS_TOKEN_KEY);

                if (profile == null || accessToken == null) {
                    throw new UnexpectedEncryptionException();
                }
                SecretKey key;
                try {
                    KeyGenerator gen = KeyGenerator.getInstance("AES");
                    gen.init(128);
                    key = gen.generateKey();
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("Failed to generate shared key.", e);
                }

                SessionService sessionService = session.getFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());
                String serverId = sessionService.getServerId(helloPacket.getServerId(), helloPacket.getPublicKey(), key);
                try {
                    sessionService.joinServer(profile, accessToken, serverId);
                } catch (ServiceUnavailableException e) {
                    session.disconnect("Login failed: Authentication service unavailable.", e);
                    return;
                } catch (InvalidCredentialsException e) {
                    session.disconnect("Login failed: Invalid login session.", e);
                    return;
                } catch (RequestException e) {
                    session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
                    return;
                }

                session.send(new ServerboundKeyPacket(helloPacket.getPublicKey(), key, helloPacket.getChallenge()));
                session.enableEncryption(key);
            } else if (packet instanceof ClientboundGameProfilePacket) {
                session.send(new ServerboundLoginAcknowledgedPacket());
            } else if (packet instanceof ClientboundLoginDisconnectPacket p) {
                session.disconnect(p.getReason());
            } else if (packet instanceof ClientboundLoginCompressionPacket p) {
                session.setCompressionThreshold(p.getThreshold(), false);
            }
        } else if (protocol.getState() == ProtocolState.STATUS) {
            if (packet instanceof ClientboundStatusResponsePacket p) {
                ServerStatusInfo info = p.getInfo();
                ServerInfoHandler handler = session.getFlag(MinecraftConstants.SERVER_INFO_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, info);
                }

                session.send(new ServerboundPingRequestPacket(System.currentTimeMillis()));
            } else if (packet instanceof ClientboundPongResponsePacket p) {
                long time = System.currentTimeMillis() - p.getPingTime();
                ServerPingTimeHandler handler = session.getFlag(MinecraftConstants.SERVER_PING_TIME_HANDLER_KEY);
                if (handler != null) {
                    handler.handle(session, time);
                }

                session.disconnect("Finished");
            }
        } else if (protocol.getState() == ProtocolState.GAME) {
            if (packet instanceof ClientboundKeepAlivePacket p && session.getFlag(MinecraftConstants.AUTOMATIC_KEEP_ALIVE_MANAGEMENT, true)) {
                session.send(new ServerboundKeepAlivePacket(p.getPingId()));
            } else if (packet instanceof ClientboundDisconnectPacket p) {
                session.disconnect(p.getReason());
            } else if (packet instanceof ClientboundStartConfigurationPacket) {
                session.send(new ServerboundConfigurationAcknowledgedPacket());
            }
        } else if (protocol.getState() == ProtocolState.CONFIGURATION) {
            if (packet instanceof ClientboundFinishConfigurationPacket) {
                session.send(new ServerboundFinishConfigurationPacket());
            } else if (packet instanceof ClientboundRegistryDataPacket p) {
                CACHE.getConfigurationCache().setRegistry(p.getRegistry());
                CACHE.getChunkCache().updateRegistryTag(p.getRegistry());
            } else if (packet instanceof ClientboundUpdateEnabledFeaturesPacket p) {
                CACHE.getConfigurationCache().setEnabledFeatures(p.getFeatures());
            } else if (packet instanceof ClientboundResourcePackPushPacket p) {
                CACHE.getConfigurationCache().getResourcePacks().put(p.getId(), new ResourcePack(p.getId(), p.getUrl(), p.getHash(), p.isRequired(), p.getPrompt()));
            } else if (packet instanceof ClientboundResourcePackPopPacket p) {
                CACHE.getConfigurationCache().getResourcePacks().remove(p.getId());
            } else if (packet instanceof ClientboundUpdateTagsPacket p) {
                CACHE.getConfigurationCache().setTags(p.getTags());
            } else if (packet instanceof ClientboundCustomPayloadPacket p) {
                if (p.getChannel().equalsIgnoreCase("minecraft:brand"))
                    CACHE.getChunkCache().setServerBrand(p.getData());
            }
        }
    }

    @Override
    public Packet packetSending(final Session session, final Packet packet) {
        try {
            return CLIENT_HANDLERS.handleOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        try {
            defaultPacketSent(session, packet);
            CLIENT_HANDLERS.handlePostOutgoing(packet, this.session);
        } catch (Exception e) {
            CLIENT_LOG.error("", e);
            throw new RuntimeException(e);
        }
    }

    private void defaultPacketSent(Session session, Packet packet) {
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (packet instanceof ClientIntentionPacket) {
            // Once the HandshakePacket has been sent, switch to the next protocol mode.
            protocol.setState(this.targetState);

            if (this.targetState == ProtocolState.LOGIN) {
                GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
                session.send(new ServerboundHelloPacket(profile.getName(), profile.getId()));
            } else {
                session.send(new ServerboundStatusRequestPacket());
            }
        } else if (packet instanceof ServerboundLoginAcknowledgedPacket) {
            protocol.setState(ProtocolState.CONFIGURATION); // LOGIN -> CONFIGURATION
        } else if (packet instanceof ServerboundFinishConfigurationPacket) {
            protocol.setState(ProtocolState.GAME); // CONFIGURATION -> GAME
        } else if (packet instanceof ServerboundConfigurationAcknowledgedPacket) {
            protocol.setState(ProtocolState.CONFIGURATION); // GAME -> CONFIGURATION
        }
    }

    @Override
    public boolean packetError(final Session session, final Throwable throwable) {
        CLIENT_LOG.debug("", throwable);
        return true;
    }

    @Override
    public void connected(final Session session) {
        CLIENT_LOG.info("Connected to {}!", session.getRemoteAddress());
        this.session.setDisconnected(false);
        MinecraftProtocol protocol = (MinecraftProtocol) session.getPacketProtocol();
        if (this.targetState == ProtocolState.LOGIN) {
            session.send(new ClientIntentionPacket(protocol.getCodec().getProtocolVersion(), session.getHost(), session.getPort(), HandshakeIntent.LOGIN));
        } else if (this.targetState == ProtocolState.STATUS) {
            session.send(new ClientIntentionPacket(protocol.getCodec().getProtocolVersion(), session.getHost(), session.getPort(), HandshakeIntent.STATUS));
        }
        EVENT_BUS.postAsync(new ConnectEvent());
    }

    @Override
    public void disconnecting(final Session session, final Component reason, final Throwable cause) {
        try {
            CLIENT_LOG.info("Disconnecting from server...");
            CLIENT_LOG.trace("Disconnect reason: {}", reason);
            // reason can be malformed for MC parser the logger uses
        } catch (final Exception e) {
            // fall through
        }
        Proxy.getInstance().getActiveConnections().forEach(connection -> connection.disconnect(reason));
    }

    @Override
    public void disconnected(final Session session, final Component reason, final Throwable cause) {
        this.session.setDisconnected(true);
        String reasonStr;
        try {
            reasonStr = ComponentSerializer.toRawString(reason);
        } catch (final Exception e) {
            CLIENT_LOG.warn("Unable to parse disconnect reason: {}", reason, e);
            reasonStr = isNull(reason) ? "Disconnected" : ComponentSerializer.serialize(reason);
        }
        CLIENT_LOG.info("Disconnected: " + reasonStr);
        EVENT_BUS.post(new DisconnectEvent(reasonStr));
    }

}
