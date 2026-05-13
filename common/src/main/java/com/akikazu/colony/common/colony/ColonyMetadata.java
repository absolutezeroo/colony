package com.akikazu.colony.common.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Lightweight, always-loaded view of a colony stored in {@link ColonyIndex}.
 *
 * <p>
 * The index avoids touching the full {@link ColonySnapshot} for cheap lookups such as "which colony owns this
 * dimension/position" or "iterate all known colonies for the world list." {@code foundedAtTick} is the only mutable-
 * looking number here, but it is fixed at registration time and never bumped afterwards — kept on the index so the
 * world list GUI can show founding age without paging in the full snapshot.
 */
public record ColonyMetadata(ResourceKey<Level> dimension, BlockPos townHallPos, long foundedAtTick)
{

    public static final Codec<ColonyMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ColonyMetadata::dimension),
            BlockPos.CODEC.fieldOf("townHallPos").forGetter(ColonyMetadata::townHallPos),
            Codec.LONG.fieldOf("foundedAtTick").forGetter(ColonyMetadata::foundedAtTick))
            .apply(instance, ColonyMetadata::new));

    public ColonyMetadata
    {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(townHallPos, "townHallPos");
    }
}
