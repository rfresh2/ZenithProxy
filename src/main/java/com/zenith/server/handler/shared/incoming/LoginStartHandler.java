/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.server.handler.shared.incoming;

import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.zenith.event.proxy.ProxyClientDisconnectedEvent;
import com.zenith.util.Wait;
import lombok.NonNull;
import com.zenith.Proxy;
import com.zenith.server.ServerConnection;
import com.zenith.util.handler.HandlerRegistry;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class LoginStartHandler implements HandlerRegistry.IncomingHandler<LoginStartPacket, ServerConnection> {
    @Override
    public boolean apply(@NonNull LoginStartPacket packet, @NonNull ServerConnection session) {
        if (CONFIG.server.extra.whitelist.enable && !isUserWhitelisted(packet.getUsername())) {
            SERVER_LOG.warn("User %s [%s] tried to connect!", packet.getUsername(), session.getRemoteAddress());
            EVENT_BUS.dispatch(new ProxyClientDisconnectedEvent("Not Whitelisted User: " + packet.getUsername() + "[" + session.getRemoteAddress() + "] tried to connect!"));
            session.disconnect(CONFIG.server.extra.whitelist.kickmsg);
            return false;
        }
        if (!Proxy.getInstance().isConnected()) {
            if (CONFIG.client.extra.autoConnectOnLogin) {
                Proxy.getInstance().connect();
                Wait.waitALittleMs(3000);
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
