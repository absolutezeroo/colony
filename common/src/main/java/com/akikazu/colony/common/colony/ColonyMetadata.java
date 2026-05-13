package com.akikazu.colony.common.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Lightweight, always-loaded view of a colony stored in {@link ColonyIndex}.
 *
 * <p>
 * The index avoids touching the full {@link ColonySnapshot} for cheap lookups such as "which colony owns this
 * dimension/position" or "iterate all known colonies for the world list."
 */
public record ColonyMetadata(ResourceKey<Level> dimension, BlockPos townHallPos)
{
    public static final Codec<ColonyMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(net.minecraft.core.registries.Registries.DIMENSION).fieldOf("dimension")
                    .forGetter(ColonyMetadata::dimension),
            BlockPos.CODEC.fieldOf("townHallPos").forGetter(ColonyMetadata::townHallPos))
            .apply(instance, ColonyMetadata::new));

    public ColonyMetadata
    {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(townHallPos, "townHallPos");
    }
}
