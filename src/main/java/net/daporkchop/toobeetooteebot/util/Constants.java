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

package net.daporkchop.toobeetooteebot.util;

import com.github.steveice10.packetlib.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.daporkchop.toobeetooteebot.client.handler.incoming.BlockChangeHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.ChatHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.ChunkDataHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.MultiBlockChangeHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.UnloadChunkHandler;
import net.daporkchop.toobeetooteebot.config.Config;
import net.daporkchop.toobeetooteebot.mc.PorkClientSession;
import net.daporkchop.toobeetooteebot.server.handler.outgoing.LoginSuccessOutgoingHandler;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author DaPorkchop_
 */
public interface Constants {
    String VERSION = "0.0.1";

    JsonParser JSON_PARSER = new JsonParser();
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    Config CONFIG = new Config("config.json");
    DataCache CACHE = new DataCache();

    AtomicBoolean SHOULD_RECONNECT = new AtomicBoolean(CONFIG.getBoolean("client.extra.autoreconnect.enabled", true));

    HandlerRegistry<PorkClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<PorkClientSession>()
            .registerInbound(new BlockChangeHandler())
            .registerInbound(new ChatHandler())
            .registerInbound(new ChunkDataHandler())
            .registerInbound(new MultiBlockChangeHandler())
            .registerInbound(new UnloadChunkHandler())
            .build();

    HandlerRegistry<Session> SERVER_HANDLERS = new HandlerRegistry.Builder<>()
            .registerOutbound(new LoginSuccessOutgoingHandler())
            .build();
}
