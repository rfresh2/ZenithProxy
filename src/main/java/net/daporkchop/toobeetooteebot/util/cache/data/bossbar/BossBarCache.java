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

package net.daporkchop.toobeetooteebot.util.cache.data.bossbar;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
public class BossBarCache implements CachedData {
    private final Map<UUID, BossBar> cachedBossBars = new ConcurrentHashMap<>();

    @Override
    public void getPacketsSimple(@NonNull Consumer<Packet> consumer) {
        this.cachedBossBars.values().stream().map(BossBar::toMCProtocolLibPacket).forEach(consumer);
    }

    @Override
    public void reset(boolean full) {
        this.cachedBossBars.clear();
    }

    @Override
    public String getSendingMessage() {
        return String.format("Sending %d boss bars", this.cachedBossBars.size());
    }

    public void add(@NonNull ServerBossBarPacket packet) {
        this.cachedBossBars.put(
                packet.getUUID(),
                new BossBar(packet.getUUID())
                        .setTitle(packet.getTitle())
                        .setHealth(packet.getHealth())
                        .setColor(packet.getColor())
                        .setDivision(packet.getDivision())
                        .setDarkenSky(packet.getDarkenSky())
                        .setDragonBar(packet.isDragonBar())
        );
    }

    public void remove(@NonNull ServerBossBarPacket packet) {
        this.cachedBossBars.remove(packet.getUUID());
    }

    public BossBar get(@NonNull ServerBossBarPacket packet) {
        BossBar bossBar = this.cachedBossBars.get(packet.getUUID());
        if (bossBar == null)    {
            return new BossBar(packet.getUUID());
        }
        return bossBar;
    }
}
