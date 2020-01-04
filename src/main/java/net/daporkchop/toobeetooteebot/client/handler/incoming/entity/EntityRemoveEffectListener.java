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

package net.daporkchop.toobeetooteebot.client.handler.incoming.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;
import lombok.NonNull;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.Entity;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.EntityEquipment;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.PotionEffect;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;

import java.util.Iterator;

import static net.daporkchop.toobeetooteebot.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class EntityRemoveEffectListener implements HandlerRegistry.IncomingHandler<ServerEntityRemoveEffectPacket, PorkClientSession> {
    @Override
    public boolean apply(@NonNull ServerEntityRemoveEffectPacket packet, @NonNull PorkClientSession session) {
        EntityEquipment entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity != null) {
            for (Iterator<PotionEffect> iterator = entity.getPotionEffects().iterator(); iterator.hasNext(); ) {
                if (iterator.next().effect == packet.getEffect()) {
                    iterator.remove();
                    break;
                }
            }
        } else {
            CLIENT_LOG.warn("Received ServerEntityRemoveEffectPacket for invalid entity (id=%d)", packet.getEntityId());
        }
        return true;
    }

    @Override
    public Class<ServerEntityRemoveEffectPacket> getPacketClass() {
        return ServerEntityRemoveEffectPacket.class;
    }
}
