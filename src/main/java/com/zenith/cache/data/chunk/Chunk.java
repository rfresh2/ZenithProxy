package com.zenith.cache.data.chunk;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.data.game.chunk.ChunkSection;
import com.github.steveice10.mc.protocol.data.game.level.LightUpdateData;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.opennbt.mini.MNBT;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Chunk {
    final int x;
    final int z;
    final ChunkSection[] sections;
    final int sectionsCount;
    final List<BlockEntityInfo> blockEntities;
    LightUpdateData lightUpdateData;
    MNBT heightMaps;

    // reusing buffer to avoid allocating extra memory
    // chunks will usually be around 10-30kb, so we can reduce gc spam by reusing the already allocated buffer
    // also, every chunk we send from the cache would be 32kb where the excess empty bytes would be unnecessary network IO
    private static final ByteBuf serializeBuffer = Unpooled.buffer();

    public byte[] serialize(MinecraftCodecHelper codec) {
        // we are limiting ourselves to one thread, but its not a big issue
        synchronized (serializeBuffer) {
            for (int i = 0; i < sections.length; i++) {
                codec.writeChunkSection(serializeBuffer, sections[i]);
            }
            var bytes = new byte[serializeBuffer.readableBytes()];
            serializeBuffer.readBytes(bytes);
            serializeBuffer.clear();
            return bytes;
        }
    }
}
