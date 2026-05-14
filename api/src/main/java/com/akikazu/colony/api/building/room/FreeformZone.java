package com.akikazu.colony.api.building.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Room footprint: a 2D set of {@link ColumnRef column references} extruded between {@code bottomY} and {@code topY}
 * (inclusive). Matches the geometry described in {@code docs/04-BUILDING-SYSTEM.md}: L-, T-, U-shapes are expressible
 * via the footprint while ceiling height is uniform.
 *
 * <p>
 * V1 ships only this footprint form for rooms — outer zones still use the simpler axis-aligned variant. The footprint
 * is validated for topological connectedness by the room painting workflow before being passed here.
 */
public record FreeformZone(Set<ColumnRef> footprint, int bottomY, int topY)
{

    public static final Codec<FreeformZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ColumnRef.CODEC.listOf().xmap(FreeformZone::asSet, List::copyOf)
                    .fieldOf("footprint").forGetter(FreeformZone::footprint),
            Codec.INT.fieldOf("bottom_y").forGetter(FreeformZone::bottomY),
            Codec.INT.fieldOf("top_y").forGetter(FreeformZone::topY))
            .apply(instance, FreeformZone::new));

    public FreeformZone
    {
        Objects.requireNonNull(footprint, "footprint");

        if (footprint.isEmpty())
        {
            throw new IllegalArgumentException("FreeformZone footprint must be non-empty");
        }

        if (bottomY > topY)
        {
            throw new IllegalArgumentException(
                    "FreeformZone requires bottomY <= topY, got bottomY=" + bottomY + " topY=" + topY);
        }

        footprint = Set.copyOf(footprint);
    }

    public static FreeformZone of(Set<ColumnRef> footprint, int bottomY, int topY)
    {
        return new FreeformZone(footprint, bottomY, topY);
    }

    private static Set<ColumnRef> asSet(List<ColumnRef> list)
    {
        return new LinkedHashSet<>(list);
    }

    public boolean contains(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        if (pos.getY() < bottomY || pos.getY() > topY)
        {
            return false;
        }

        return footprint.contains(new ColumnRef(pos.getX(), pos.getZ()));
    }

    public int volume()
    {
        return footprint.size() * (topY - bottomY + 1);
    }

    public Stream<BlockPos> blocksInZone()
    {
        return footprint.stream().flatMap(this::columnBlocks);
    }

    private Stream<BlockPos> columnBlocks(ColumnRef column)
    {
        Stream.Builder<BlockPos> builder = Stream.builder();

        for (int y = bottomY; y <= topY; y++)
        {
            builder.add(new BlockPos(column.x(), y, column.z()));
        }

        return builder.build();
    }

    public Stream<ChunkPos> chunks()
    {
        return footprint.stream()
                .map(c -> new ChunkPos(c.x() >> 4, c.z() >> 4))
                .distinct();
    }

    /**
     * Lightweight (x, z) tuple used to express a 2D footprint. Vanilla's {@code ColumnPos} would also fit but is in a
     * package consumers don't necessarily want to import, and we want a record we can codec.
     */
    public record ColumnRef(int x, int z)
    {
        public static final Codec<ColumnRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(ColumnRef::x),
                Codec.INT.fieldOf("z").forGetter(ColumnRef::z))
                .apply(instance, ColumnRef::new));
    }
}
