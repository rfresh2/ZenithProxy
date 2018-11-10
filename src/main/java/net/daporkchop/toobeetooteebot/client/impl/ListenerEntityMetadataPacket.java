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

package net.daporkchop.toobeetooteebot.client.impl;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.packetlib.Session;
import com.google.common.collect.Lists;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.client.IPacketListener;
import net.daporkchop.toobeetooteebot.entity.api.Entity;

import java.util.ArrayList;

public class ListenerEntityMetadataPacket implements IPacketListener<ServerEntityMetadataPacket> {
    @Override
    public void handlePacket(Session session, ServerEntityMetadataPacket pck) {
        Entity entity = Caches.getEntityByEID(pck.entityId);
        ArrayList<EntityMetadata> oldMeta = Lists.newArrayList(entity.metadata);
        ArrayList<EntityMetadata> newMeta = new ArrayList<>();
        OLDCHECK:
        for (EntityMetadata oldCheck : oldMeta) { //add old fields and merge
            for (EntityMetadata newCheck : pck.metadata) {
                if (newCheck.id == oldCheck.id) {
                    newMeta.add(newCheck);
                    continue OLDCHECK;
                }
            }
            newMeta.add(oldCheck);
        }
        NEWCHECK:
        for (EntityMetadata newCheck : pck.metadata) {
            for (EntityMetadata oldCheck : oldMeta) {
                if (oldCheck.id == newCheck.id) {
                    continue NEWCHECK;
                }
            }

            newMeta.add(newCheck);
        }
        entity.metadata = newMeta.toArray(new EntityMetadata[newMeta.size()]);
    }
}
