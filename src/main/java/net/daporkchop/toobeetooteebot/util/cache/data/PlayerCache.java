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

package net.daporkchop.toobeetooteebot.util.cache.data;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.toobeetooteebot.util.cache.CachedData;
import net.daporkchop.toobeetooteebot.util.cache.data.entity.EntityPlayer;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * @author DaPorkchop_
 */
@Getter
@Setter
@Accessors(chain = true)
public class PlayerCache implements CachedData {
    protected boolean hardcore;
    protected boolean reducedDebugInfo;
    protected int maxPlayers;
    protected int dimension;
    @NonNull
    protected GameMode gameMode;
    @NonNull
    protected WorldType worldType;
    @NonNull
    protected Difficulty difficulty;
    
    protected EntityPlayer thePlayer;

    protected final ItemStack[] inventory = new ItemStack[46];

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ServerPlayerPositionRotationPacket(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), ThreadLocalRandom.current().nextInt(16, 1024)));
        consumer.accept(new ServerWindowItemsPacket(0, this.inventory.clone()));
    }

    @Override
    public void reset(boolean full) {
        if (full)   {
            this.thePlayer = (EntityPlayer) new EntityPlayer(true).setEntityId(-1);
            this.hardcore = this.reducedDebugInfo = false;
            this.maxPlayers = -1;
            Arrays.fill(this.inventory, null);
        }
        this.dimension = Integer.MAX_VALUE;
        this.gameMode = null;
        this.worldType = null;
        this.difficulty = null;
    }

    @Override
    public String getSendingMessage() {
        return String.format(
                "Sending player position: (x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f)",
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getYaw(),
                this.getPitch()
        );
    }

    public double getX()    {
        return this.thePlayer.getX();
    }

    public PlayerCache setX(double x)    {
        this.thePlayer.setX(x);
        return this;
    }

    public double getY()    {
        return this.thePlayer.getY();
    }

    public PlayerCache setY(double y)    {
        this.thePlayer.setY(y);
        return this;
    }

    public double getZ()    {
        return this.thePlayer.getZ();
    }

    public PlayerCache setZ(double z)    {
        this.thePlayer.setZ(z);
        return this;
    }

    public float getYaw()    {
        return this.thePlayer.getYaw();
    }

    public PlayerCache setYaw(float yaw)    {
        this.thePlayer.setYaw(yaw);
        return this;
    }

    public float getPitch()    {
        return this.thePlayer.getPitch();
    }

    public PlayerCache setPitch(float pitch)    {
        this.thePlayer.setPitch(pitch);
        return this;
    }

    public int getEntityId()    {
        return this.thePlayer.getEntityId();
    }

    public PlayerCache setEntityId(int id)  {
        if (this.thePlayer.getEntityId() != -1) {
            CACHE.getEntityCache().remove(this.thePlayer.getEntityId());
        }
        this.thePlayer.setEntityId(id);
        CACHE.getEntityCache().add(this.thePlayer);
        return this;
    }
}
