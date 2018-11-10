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

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.Constants;

import java.net.Proxy;

/**
 * @author DaPorkchop_
 */
public class MinecraftProtocolWrapper extends MinecraftProtocol implements Constants {
    private final Bot bot;

    public MinecraftProtocolWrapper(SubProtocol subProtocol, @NonNull Bot bot) {
        super(subProtocol);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(String username, @NonNull Bot bot) {
        super(username);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(String username, String password, @NonNull Bot bot) throws RequestException {
        super(username, password);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(String username, String clientToken, String accessToken, @NonNull Bot bot) throws RequestException {
        super(username, clientToken, accessToken);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(String username, String password, Proxy proxy, @NonNull Bot bot) throws RequestException {
        super(username, password, proxy);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(String username, String clientToken, String accessToken, Proxy proxy, @NonNull Bot bot) throws RequestException {
        super(username, clientToken, accessToken, proxy);
        this.bot = bot;
    }

    public MinecraftProtocolWrapper(GameProfile profile, String clientToken, String accessToken, @NonNull Bot bot) {
        super(profile, clientToken, accessToken);
        this.bot = bot;
    }

    @Override
    public void newServerSession(Server server, Session session) {
        super.newServerSession(server, session);
        if (CONFIG.getBoolean("server.enabled"))     {
            session.addListener(new PorkServerConnection(this.bot, session));
        }
    }
}
