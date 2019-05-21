/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.websocket;

import lombok.NonNull;
import net.daporkchop.toobeetooteebot.util.Constants;
import net.daporkchop.toobeetooteebot.util.cache.data.tab.PlayerEntry;
import org.apache.commons.text.StringEscapeUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * @author DaPorkchop_
 */
public class WebSocketServer extends org.java_websocket.server.WebSocketServer implements Constants {
    public static final boolean ENABLED = CONFIG.getBoolean("websocket.enable", false);
    protected static final int MAX_CHAT_COUNT = CONFIG.getInt("websocket.client.maxChatCount", 512);
    protected final String[] messages = ENABLED ? new String[MAX_CHAT_COUNT] : null;

    public WebSocketServer() {
        super(new InetSocketAddress(CONFIG.getString("websocket.bind.host", "0.0.0.0"), CONFIG.getInt("websocket.bind.port", 8080)));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send(String.format(
                "{\"command\":\"init\",\"maxChatCount\":%d}",
                MAX_CHAT_COUNT
        ));
        CACHE.getTabListCache().getTabList().getEntries().stream().map(this::getUpdatePlayerCommand).forEach(conn::send);

        synchronized (this.messages) {
            Arrays.stream(this.messages).filter(Objects::nonNull).forEachOrdered(conn::send);
        }
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
        }
    }

    @Override
    public void onStart() {
        WEBSOCKET_LOG.success("WebSocket server started!");
    }

    public void fireChat(@NonNull String message) {
        if (ENABLED) {
            message = String.format(
                    "{\"command\":\"chat\",\"chat\":%s}",
                    message
            );
            synchronized (this.messages) {
                for (int i = 1; i < MAX_CHAT_COUNT; i++) {
                    this.messages[i - 1] = this.messages[i];
                }
                this.messages[MAX_CHAT_COUNT - 1] = message;
                this.broadcast(message);
            }
        }
    }

    public void updatePlayer(@NonNull PlayerEntry entry)    {
        if (ENABLED)    {
            this.broadcast(this.getUpdatePlayerCommand(entry));
        }
    }

    public void removePlayer(@NonNull UUID id)    {
        if (ENABLED)    {
            this.broadcast(String.format("{\"command\":\"removePlayer\",\"uuid\":\"%s\"}", id.toString()));
        }
    }

    public void shutdown()  {
        if (ENABLED)    {
            try {
                this.stop();
            } catch (Exception e)   {
                throw new RuntimeException(e);
            }
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
}
