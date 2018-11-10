/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
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

package net.daporkchop.toobeetooteebot.mc;

import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.client.ClientListener;

import java.util.concurrent.CompletableFuture;

/**
 * @author DaPorkchop_
 */
@Getter
public class PorkClientSession extends TcpClientSession {
    @Getter(AccessLevel.PRIVATE)
    private final CompletableFuture<String> disconnectFuture = new CompletableFuture<>();
    private final Bot bot;

    public PorkClientSession(String host, int port, PacketProtocol protocol, Client client, @NonNull Bot bot) {
        super(host, port, protocol, client, null);
        this.bot = bot;
        this.addListener(new ClientListener(this.bot, this));
    }

    public String getDisconnectReason() {
        try {
            return this.disconnectFuture.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnect(String reason, Throwable cause, boolean wait) {
        super.disconnect(reason, cause, wait);
        if (cause == null) {
            this.disconnectFuture.complete(reason);
        } else {
            this.disconnectFuture.completeExceptionally(cause); //TODO: maybe just print stack trace? not sure what exceptions might be given to this
        }
    }
}
