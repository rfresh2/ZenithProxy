package com.zenith.feature.coordobf;

import com.viaversion.nbt.io.MNBTIO;
import com.viaversion.nbt.mini.MNBT;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.Tag;
import com.zenith.feature.player.World;
import com.zenith.mc.biome.Biome;
import com.zenith.mc.biome.BiomeRegistry;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.mc.item.ItemRegistry;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.PalettedWorldState;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.PaletteType;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.SingletonPalette;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ItemParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.VibrationParticleData;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.positionsource.BlockPositionSource;

import java.security.SecureRandom;
import java.util.*;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.SERVER_LOG;

/**
 * Important to deep copy all objects that are offset by this class
 * Each session will have its own CoordOffset instance. Packet data must not be mutated in-place.
 */
public record CoordOffset(
    // in chunk coordinate offset
    int x,
    int z
) {
    static final SecureRandom RANDOM = new SecureRandom();
    public static final double EPSILON = 0.0001;
    public double obfuscateDouble(double d) {
        // obfuscate double precision to prevent possible exploits
        // this will cause values to be slightly off, but not enough to be an issue
        return d + RANDOM.nextDouble(-EPSILON, EPSILON);
    }
    public int offsetX(final int x) {
        return x + (x() * 16);
    }
    public double offsetX(final double x) {
        return obfuscateDouble(x + (x() * 16));
    }
    public int offsetChunkX(final int x) {
        return x + x();
    }
    public int offsetZ(final int z) {
        return z + (z() * 16);
    }
    public double offsetZ(final double z) {
        return obfuscateDouble(z + (z() * 16));
    }
    public int offsetChunkZ(final int z) {
        return z + z();
    }
    public int reverseOffsetX(final int x) {
        return x - (x() * 16);
    }
    public double reverseOffsetX(final double x) {
        return x - (x() * 16);
    }
    public int reverseChunkOffsetX(final int x) {
        return x - x();
    }
    public int reverseOffsetZ(final int z) {
        return z - (z() * 16);
    }
    public double reverseOffsetZ(final double z) {
        return z - (z() * 16);
    }
    public int reverseChunkOffsetZ(final int z) {
        return z - z();
    }
    public Vector3i offsetVector(final Vector3i vec) {
        return vec.add(x() * 16, 0, z() * 16);
    }
    public Vector3i reverseOffsetVector(final Vector3i vec) {
        return vec.sub(x() * 16, 0, z() * 16);
    }
    public MNBT offsetNbt(final MNBT nbt) {
        try {
            return MNBTIO.write(offsetNbt(MNBTIO.read(nbt)), false);
        } catch (final Throwable e) {
            SERVER_LOG.error("Failed to offset NBT", e);
            return nbt;
        }
    }
    public Tag offsetNbt(final Tag tag) {
        var out = tag;
        if (tag instanceof CompoundTag compoundTag) {
            out = offsetCompoundTag(compoundTag);
        }
        return out;
    }
    // this is very inefficient, causes a lot of memory copies in deep NBT structures
    public CompoundTag offsetCompoundTag(final CompoundTag compoundTag) {
        var out = compoundTag.copy(); // recursive copy
        for (Map.Entry<String, Tag> entry : out) {
            var tag = entry.getValue();
            if (tag instanceof CompoundTag childCompoundTag) {
                out.put(entry.getKey(), offsetCompoundTag(childCompoundTag));
            } else if (tag instanceof IntTag intTag) {
                // todo: are there more keys with coords in tags that need to be offset?
                IntTag result = intTag;
                if (entry.getKey().equals("x")) {
                    result = new IntTag(offsetX(intTag.asInt()));
                } else if (entry.getKey().equals("z")) {
                    result = new IntTag(offsetZ(intTag.asInt()));
                }
                if (result != intTag)
                    out.put(entry.getKey(), result);
            } else if (tag instanceof ListTag listTag) {
                var list = listTag.getValue();
                for (int i = 0; i < list.size(); i++) {
                    var listTagValue = list.get(i);
                    if (listTagValue instanceof CompoundTag listCompoundTag) {
                        list.set(i, offsetCompoundTag(listCompoundTag));
                    }
                }
            }
        }
        return out;
    }
    public BlockEntityInfo offsetBlockEntityInfo(final BlockEntityInfo blockEntityInfo) {
        return new BlockEntityInfo(
            blockEntityInfo.getX(), // these should be in chunk relative coords (0-15) so don't offset them
            blockEntityInfo.getY(),
            blockEntityInfo.getZ(),
            blockEntityInfo.getType(),
            blockEntityInfo.getNbt() == null ? null : offsetNbt(blockEntityInfo.getNbt())
        );
    }
    public BlockEntityInfo[] offsetBlockEntityInfos(final BlockEntityInfo[] blockEntityInfos) {
        var out = new BlockEntityInfo[blockEntityInfos.length];
        for (int i = 0; i < blockEntityInfos.length; i++) {
            out[i] = offsetBlockEntityInfo(blockEntityInfos[i]);
        }
        return out;
    }

    private static final ReferenceSet<DataComponentType<?>> componentsToStrip = ReferenceOpenHashSet.of(
        // not all of these necessarily are dangerous, but i haven't actually checked all component types for safety yet
        DataComponentTypes.CUSTOM_DATA,
        DataComponentTypes.MAP_DECORATIONS,
        DataComponentTypes.ENTITY_DATA,
        DataComponentTypes.BUCKET_ENTITY_DATA,
        DataComponentTypes.BLOCK_ENTITY_DATA,
        DataComponentTypes.LODESTONE_TRACKER,
        DataComponentTypes.CHARGED_PROJECTILES,
        DataComponentTypes.BUNDLE_CONTENTS
    );

    // 2b2t seems to remove most tags already but just to be safe
    public ItemStack sanitizeItemStack(final ItemStack itemStack) {
        if (itemStack == null) return null;
        if (itemStack.getId() == ItemRegistry.COMPASS.id()
            || itemStack.getId() == ItemRegistry.RECOVERY_COMPASS.id()
            || itemStack.getId() == ItemRegistry.BEEHIVE.id()
            || itemStack.getId() == ItemRegistry.FILLED_MAP.id()
        ) {
            // if the tag has a real coord known, offsetting it would reveal the offset
            // better to be safe here and just remove the tag
            return new ItemStack(itemStack.getId(), itemStack.getAmount(), null);
        }
        if (itemStack.getDataComponents() != null) {
            Map<DataComponentType<?>, DataComponent<?, ?>> components = new HashMap<>(itemStack.getDataComponents().getDataComponents());
            for (var it = components.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                if (componentsToStrip.contains(entry.getKey())) {
                    it.remove();
                    continue;
                }
                if (entry.getKey() == DataComponentTypes.CONTAINER) {
                    var containerDataComponent = entry.getValue();
                    var containerDataComponentValue = containerDataComponent.getValue();
                    if (containerDataComponentValue != null) {
                        var itemStacks = (List<ItemStack>) containerDataComponent.getValue();
                        var copiedItemStacks = new ArrayList<ItemStack>(itemStacks.size());
                        for (var itemStackValue : itemStacks) {
                            // recursively sanitize item stacks in the container
                            var newStack = sanitizeItemStack(itemStackValue);
                            if (newStack != null) {
                                copiedItemStacks.add(newStack);
                            }
                        }
                        var newDataComponent = DataComponentTypes.CONTAINER.getDataComponentFactory()
                            .create(DataComponentTypes.CONTAINER, copiedItemStacks);
                        entry.setValue(newDataComponent);
                    }
                }
            }
            return new ItemStack(itemStack.getId(), itemStack.getAmount(), new DataComponents(components));
        }
        return itemStack;
    }

    public ChunkSection[] obfuscateChunkSections(final ChunkSection[] originalSections) {
        var currentDimension = World.getCurrentDimension();
        var shouldAddBedrockLayer = CONFIG.client.extra.coordObfuscation.obfuscateBedrock
            && (currentDimension == DimensionRegistry.OVERWORLD.get() || currentDimension == DimensionRegistry.THE_NETHER.get());
        var shouldReplaceBiomes = CONFIG.client.extra.coordObfuscation.obfuscateBiomes;
        DataPalette obfuscatedBiomes = null;
        if (shouldReplaceBiomes && originalSections.length > 0) {
            obfuscatedBiomes = obfuscatedBiomePalette(originalSections[0].getBiomeData().getPalettedWorldState());
        }

        if (!shouldAddBedrockLayer && !shouldReplaceBiomes) return originalSections;

        var nether = currentDimension == DimensionRegistry.THE_NETHER.get();
        // not a deep copy of the sections, only a shallow copy of the array
        var sections = Arrays.copyOf(originalSections, originalSections.length);
        for (int i = 0; i < sections.length; i++) {
            var bedrockInThisSection = shouldAddBedrockLayer && (i == 0 || (nether && i == 7));
            if (bedrockInThisSection || shouldReplaceBiomes) {
                var newSection = new ChunkSection(sections[i]);
                if (bedrockInThisSection) {
                    if (i == 0) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < 5; y++) {
                                    newSection.setBlock(x, y, z, BlockRegistry.BEDROCK.minStateId());
                                }
                            }
                        }
                    } else if (nether && i == 7) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 11; y < 16; y++) {
                                    newSection.setBlock(x, y, z, BlockRegistry.BEDROCK.minStateId());
                                }
                            }
                        }
                    }
                }
                if (shouldReplaceBiomes) {
                    newSection.setBiomeData(obfuscatedBiomes);
                }
                sections[i] = newSection;
            }
        }
        return sections;
    }

    public DataPalette[] obfuscateBiomePalettes(final DataPalette[] originalPalettes) {
        if (!CONFIG.client.extra.coordObfuscation.obfuscateBiomes) return originalPalettes;
        if (originalPalettes.length == 0) return originalPalettes;
        var palettes = new DataPalette[originalPalettes.length];
        Arrays.fill(palettes, obfuscatedBiomePalette(originalPalettes[0].getPalettedWorldState()));
        return palettes;
    }

    public DataPalette obfuscatedBiomePalette(PalettedWorldState palettedWorldState) {
        String biomeKey = CONFIG.client.extra.coordObfuscation.obfuscateBiomesKey;
        Biome biome = BiomeRegistry.REGISTRY.get(biomeKey);
        int id = 40;
        if (biome != null) {
            id = biome.id();
        }
        return new DataPalette(new SingletonPalette(id), null, PaletteType.BIOME, palettedWorldState);
    }

    public Particle offsetParticle(Particle particle) {
        if (particle == null) return particle;
        if (particle.getData() instanceof ItemParticleData itemParticleData) {
            particle = new Particle(
                particle.getType(),
                new ItemParticleData(
                    sanitizeItemStack(itemParticleData.getItemStack())
                )
            );
        } else if (particle.getData() instanceof VibrationParticleData vibrationParticleData) {
            var positionSrc = vibrationParticleData.getPositionSource();
            if (positionSrc instanceof BlockPositionSource bps) {
                positionSrc = new BlockPositionSource(
                    offsetVector(bps.getPosition())
                );
            }
            particle = new Particle(
                particle.getType(),
                new VibrationParticleData(
                    positionSrc,
                    vibrationParticleData.getArrivalTicks()
                )
            );
        }
        return particle;
    }
}
