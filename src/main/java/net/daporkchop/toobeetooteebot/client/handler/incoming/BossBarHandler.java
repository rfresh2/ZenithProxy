/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2020 DaPorkchop_
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

package net.daporkchop.toobeetooteebot.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.function.Consumer;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class BossBarHandler implements HandlerRegistry.IncomingHandler<ServerBossBarPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerBossBarPacket pck, @NonNull PorkClientSession session) {
        Consumer<ServerBossBarPacket> consumer = packet -> {
            throw new IllegalStateException();
        };
        switch (pck.getAction())    {
            case ADD:
                consumer = CACHE.getBossBarCache()::add;
                break;
            case REMOVE:
                consumer = CACHE.getBossBarCache()::remove;
                break;
            case UPDATE_HEALTH:
                consumer = packet -> CACHE.getBossBarCache().get(packet).setHealth(packet.getHealth());
                break;
            case UPDATE_TITLE:
                consumer = packet -> CACHE.getBossBarCache().get(packet).setTitle(packet.getTitle());
                break;
            case UPDATE_STYLE:
                consumer = packet -> CACHE.getBossBarCache().get(packet).setColor(packet.getColor()).setDivision(packet.getDivision());
                break;
            case UPDATE_FLAGS:
                consumer = packet -> CACHE.getBossBarCache().get(packet).setDarkenSky(packet.getDarkenSky()).setDragonBar(packet.isDragonBar());
                break;
        }
        consumer.accept(pck);
        return true;
    }

    @Override
    public Class<ServerBossBarPacket> getPacketClass() {
        return ServerBossBarPacket.class;
    }
}
