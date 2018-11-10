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

package net.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.SessionFactory;
import lombok.Getter;
import net.daporkchop.toobeetooteebot.mc.PorkClientSession;
import net.daporkchop.toobeetooteebot.mc.PorkSessionFactory;
import net.daporkchop.toobeetooteebot.util.Constants;

/**
 * @author DaPorkchop_
 */
@Getter
public class Bot implements Constants {
    @Getter
    private static Bot instance;

    private MinecraftProtocol protocol;
    private Client client;
    private final SessionFactory sessionFactory = new PorkSessionFactory(this);

    public static void main(String... args) {
        System.out.printf("Starting Pork2b2tBot v%s...\n", VERSION);

        Bot bot = new Bot();
        instance = bot;
        bot.start();
    }

    public void start() {
        do {
            this.logIn();
            this.connect();

            CONFIG.save();
            //wait for client to disconnect before starting again
            System.out.printf("Disconnected. Reason: %s\n", ((PorkClientSession) this.client.getSession()).getDisconnectReason());
            CONFIG.save();
        } while (SHOULD_RECONNECT.get() && CACHE.reset() && this.delayBeforeReconnect());
        System.out.println("Shutting down...");
    }

    private void connect() {
        synchronized (this) {
            if (this.isConnected()) {
                throw new IllegalStateException("Already connected!");
            }

            String address = CONFIG.getString("client.server.address", "mc.example.com");
            int port = CONFIG.getInt("client.server.port", 25565);

            System.out.printf("Connecting to %s:%d...\n", address, port);
            this.client = new Client(address, port, this.protocol, this.sessionFactory);
            this.client.getSession().connect(true);
        }
    }

    public boolean isConnected() {
        return this.client != null && this.client.getSession() != null && this.client.getSession().isConnected();
    }

    private void logIn() {
        if (this.protocol == null) {
            System.out.println("Logging in...");
            if (CONFIG.getBoolean("authentication.doAuthentication")) {
                try {
                    this.protocol = new MinecraftProtocol(
                            CONFIG.getString("authentication.username", "john.doe@example.com"),
                            CONFIG.getString("authentication.password", "hackme")
                    );
                } catch (RequestException e) {
                    throw new RuntimeException(String.format(
                            "Unable to log in using credentials %s:%s",
                            CONFIG.getString("authentication.username"),
                            CONFIG.getString("authentication.password")), e);
                }
            } else {
                this.protocol = new MinecraftProtocol(CONFIG.getString("authentication.username", "Steve"));
                CONFIG.getString("authentication.password", "hackme"); //add password field to config by default
            }
            System.out.println("Successfully logged in.");
        }
    }

    private boolean delayBeforeReconnect()  {
        try {
            for (int i = CONFIG.getInt("client.extra.autoreconnect.delay", 10); i > 0; i--) {
                System.out.printf("Reconnecting in %d\n", i);
                Thread.sleep(1000L);
            }
        } catch ( InterruptedException e)   {
            throw new RuntimeException(e);
        } finally {
            return true;
        }
    }
}
