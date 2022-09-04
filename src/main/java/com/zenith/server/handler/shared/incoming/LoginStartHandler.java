package com.zenith.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.zenith.Proxy;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import static com.zenith.util.Constants.*;

public class LoginStartHandler implements HandlerRegistry.IncomingHandler<LoginStartPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull LoginStartPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.server.extra.whitelist.enable && !isUserWhitelisted(packet.getUsername())) {
            SERVER_LOG.warn("User {} [{}] tried to connect!", packet.getUsername(), session.getRemoteAddress());
            EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent("Not Whitelisted User: " + packet.getUsername() + "[" + session.getRemoteAddress() + "] tried to connect!"));
            session.disconnect(CONFIG.server.extra.whitelist.kickmsg);
            return false;
        }
        if (!Proxy.getInstance().isConnected()) {
            if (CONFIG.client.extra.autoConnectOnLogin) {
                Proxy.getInstance().connect();
            } else {
                session.disconnect("Not connected to server!");
            }
        }
        return false;
    }

    public boolean isUserWhitelisted(String loggingInUser) {
        return CONFIG.server.extra.whitelist.allowedUsers.stream()
                .anyMatch(user -> user.equalsIgnoreCase(loggingInUser));
    }

    @Override
    public Class<LoginStartPacket> getPacketClass() {
        return LoginStartPacket.class;
    }
}
