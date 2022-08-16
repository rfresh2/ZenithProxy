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

package com.zenith.websocket;

import lombok.NonNull;
import com.zenith.cache.data.tab.PlayerEntry;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static com.zenith.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    protected final String[] messages = CONFIG.websocket.enable ? new String[CONFIG.websocket.client.maxChatCount] : null;

    public WebSocketServer() {
        super(new InetSocketAddress(CONFIG.websocket.bind.address, CONFIG.websocket.bind.port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send(String.format(
                "{\"command\":\"init\",\"maxChatCount\":%d}",
                CONFIG.websocket.client.maxChatCount
        ));
        CACHE.getTabListCache().getTabList().getEntries().stream().map(this::getUpdatePlayerCommand).forEach(conn::send);

        synchronized (this.messages) {
            Arrays.stream(this.messages).filter(Objects::nonNull).forEachOrdered(conn::send);
        }
        conn.send(this.getTabDataCommand());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        WEBSOCKET_LOG.alert(ex);
        if (conn != null) {
            conn.close();
        } else {
            System.exit(1);
        }
    }

    @Override
    public void onStart() {
        WEBSOCKET_LOG.success("WebSocket server started!");
    }

    public void fireChat(@NonNull String message) {
        if (CONFIG.websocket.enable) {
            message = String.format(
                    "{\"command\":\"chat\",\"chat\":%s}",
                    message
            );
            synchronized (this.messages) {
                System.arraycopy(this.messages, 1, this.messages, 0, this.messages.length - 1);
                this.messages[this.messages.length - 1] = message;
                this.broadcast(message);
            }
        }
    }

    public void fireReset() {
        if (CONFIG.websocket.enable)    {
            this.broadcast("{\"command\":\"reset\"}");
        }
    }

    public void firePlayerListUpdate() {
        if (CONFIG.websocket.enable) {
            this.broadcast(this.getTabDataCommand());
        }
    }

    public void updatePlayer(@NonNull PlayerEntry entry) {
        if (CONFIG.websocket.enable) {
            this.broadcast(this.getUpdatePlayerCommand(entry));
        }
    }

    public void removePlayer(@NonNull UUID id) {
        if (CONFIG.websocket.enable) {
            this.broadcast(String.format("{\"command\":\"removePlayer\",\"uuid\":\"%s\"}", id.toString()));
        }
    }

    public void shutdown() {
        if (CONFIG.websocket.enable) {
            WEBSOCKET_LOG.info("Shutting down...");
            try {
                this.stop(5000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            WEBSOCKET_LOG.success("Shut down.");
        }
    }

    protected String getUpdatePlayerCommand(@NonNull PlayerEntry entry) {
        return String.format(
                "{\"command\":\"player\",\"name\":\"%s\",\"uuid\":\"%s\",\"ping\":%d}",
                entry.getName(),
                entry.getId().toString(),
                entry.getPing()
        );
    }

    protected String getTabDataCommand() {
        return String.format(
                "{\"command\":\"tabData\",\"header\":%s,\"footer\":%s}",
                CACHE.getTabListCache().getTabList().getHeader(),
                CACHE.getTabListCache().getTabList().getFooter()
        );
    }
}
