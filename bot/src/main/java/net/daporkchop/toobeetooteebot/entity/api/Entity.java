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

package net.daporkchop.toobeetooteebot.entity.api;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.entity.EntityType;

import java.util.UUID;

public abstract class Entity {
    public EntityType type;
    public double x, y, z;
    public int entityId;
    public UUID uuid;
    public EntityMetadata metadata[] = new EntityMetadata[0];
    public int passengerIds[] = {};

    public static Entity getEntityByID(int id) {
        for (Entity entity : Caches.cachedEntities.values()) {
            if (entity.entityId == id) {
                return entity;
            }
        }

        return null;
    }

    public static Entity getEntityBeingRiddenBy(int entityId) {
        for (Entity entity1 : Caches.cachedEntities.values()) {
            for (int pID : ((EntityEquipment) entity1).passengerIds) {
                if (pID == entityId) {
                    return entity1;
                }
            }
        }

        return null;
    }
}
