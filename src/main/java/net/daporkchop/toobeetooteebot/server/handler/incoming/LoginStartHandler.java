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

package net.daporkchop.toobeetooteebot.server.handler.incoming;

import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.Bot;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.List;

/**
 * @author DaPorkchop_
 */
public class LoginStartHandler implements HandlerRegistry.IncomingHandler<LoginStartPacket, PorkServerConnection> {
    //TODO: better way of doing this?
    static {
        //this just adds the default values to the config

        CONFIG.getBoolean("server.extra.whitelist.enable");
        JsonArray def = new JsonArray();
        def.add(new JsonPrimitive("DaPorkchop_"));
        //def.add(new JsonPrimitive("069a79f4-44e9-4726-a5be-fca90e38aaf5")); //TODO: support UUIDs in whitelist
        CONFIG.getArray("server.extra.whitelist.allowedusers", def);
        CONFIG.getString("server.extra.whitelist.kickmsg", "get out of here you HECKING scrub");
    }

    @Override
    public boolean apply(@NonNull LoginStartPacket packet, @NonNull PorkServerConnection session) {
        if (CONFIG.getBoolean("server.extra.whitelist.enable")) {
            List<String> whitelist = CONFIG.getList("server.extra.whitelist.allowedusers", JsonElement::getAsString);
            if (!whitelist.contains(packet.getUsername())) {
                SERVER_LOG.warn("User %s [%s] tried to connect!", packet.getUsername(), session.getRemoteAddress());
                session.disconnect(CONFIG.getString("server.extra.whitelist.kickmsg"));
                return false;
            }
        }
        if (!Bot.getInstance().isConnected())   {
            session.disconnect("Not connected to server!");
        }
        return false;
    }

    @Override
    public Class<LoginStartPacket> getPacketClass() {
        return LoginStartPacket.class;
    }
}
