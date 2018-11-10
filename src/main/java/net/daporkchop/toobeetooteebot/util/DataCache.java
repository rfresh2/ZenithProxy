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

import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import lombok.Getter;
import net.daporkchop.lib.math.vector.d.Vec3dM;
import net.daporkchop.lib.primitive.map.LongObjectMap;
import net.daporkchop.lib.primitive.map.PorkMaps;
import net.daporkchop.lib.primitive.map.hashmap.LongObjectHashMap;

/**
 * @author DaPorkchop_
 */
@Getter
public class DataCache implements Constants {
    public DataCache() {
        this.reset();
    }

    private final LongObjectMap<Column> chunks = PorkMaps.synchronize(new LongObjectHashMap<>());
    private final Vec3dM playerPos = new Vec3dM(0.0d, 0.0d, 0.0d);

    public boolean reset() {
        System.out.println("Clearing cache...");

        try {
            this.chunks.clear();

            this.playerPos.setX(0.0d);
            this.playerPos.setY(0.0d);
            this.playerPos.setZ(0.0d);

            System.out.println("Cache cleared.");
        } catch (Exception e)   {
            throw new RuntimeException("Unable to clear cache", e);
        }
        return true;
    }
}
