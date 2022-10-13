package com.zenith.pathing;

import com.google.gson.reflect.TypeToken;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;

import static com.zenith.util.Constants.GSON;

public class BlockDataManager {
    private final List<Block> blocks;

    public BlockDataManager() {
        File f = new File(this.getClass().getClassLoader().getResource("data/pc/1.12/blocks.json").getFile());
        try (Reader reader = new UTF8FileReader(f)) {
            blocks = GSON.fromJson(reader, TypeToken.getParameterized(List.class, Block.class).getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Block> getBlockFromId(int id) {
        return blocks.stream().filter(b -> b.getId() == id).findFirst();
    }
}
