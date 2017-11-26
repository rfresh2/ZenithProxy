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

package net.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import net.daporkchop.toobeetooteebot.entity.api.Entity;
import net.daporkchop.toobeetooteebot.entity.impl.EntityPlayer;
import net.daporkchop.toobeetooteebot.util.EntityNotFoundException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.UUID;

public class Caches {
    public static double x = 0;
    public static double y = 0;
    public static double z = 0;
    public static float yaw = 0;
    public static float pitch = 0;
    public static int dimension = 0;
    public static int eid = 0;
    public static GameMode gameMode = GameMode.SURVIVAL;
    public static boolean onGround;
    public static EntityPlayer player;
    public static UUID uuid;
    public static HashMap<Long, Column> cachedChunks = new HashMap<>();
    public static HashMap<Integer, Entity> cachedEntities = new HashMap<>();
    public static HashMap<UUID, ServerBossBarPacket> cachedBossBars = new HashMap<>();
    public static BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

    static {
        Graphics g2 = icon.getGraphics();
        g2.setColor(new Color(0, 0, 0, 1));
        g2.fillRect(0, 0, 256, 256);
    }

    public static void updatePlayerLocRot() {
        player.x = x;
        player.y = y;
        player.z = z;
        player.yaw = yaw;
        player.pitch = pitch;
    }

    public static Entity getEntityByEID(int eid) {
        Entity entity = cachedEntities.get(eid);
        if (entity == null) {
            throw new EntityNotFoundException();
        }
        return entity;
    }
}
