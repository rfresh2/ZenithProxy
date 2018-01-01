/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot.client.impl;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.github.steveice10.packetlib.Session;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.client.IPacketListener;

public class ListenerBossBarPacket implements IPacketListener<ServerBossBarPacket> {
    @Override
    public void handlePacket(Session session, ServerBossBarPacket pck) {
        switch (pck.action) {
            case ADD:
                Caches.cachedBossBars.put(pck.uuid, pck);
                break;
            case REMOVE:
                Caches.cachedBossBars.remove(pck.uuid);
                break;
            case UPDATE_HEALTH:
                Caches.cachedBossBars.get(pck.uuid).health = pck.health;
                break;
            case UPDATE_TITLE:
                Caches.cachedBossBars.get(pck.uuid).title = pck.title;
                break;
            case UPDATE_STYLE:
                Caches.cachedBossBars.get(pck.uuid).color = pck.color;
                break;
            case UPDATE_FLAGS:
                Caches.cachedBossBars.get(pck.uuid).darkenSky = pck.darkenSky;
                break;
        }
    }
}
