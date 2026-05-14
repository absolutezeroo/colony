package com.akikazu.colony.api.workzone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Rectangular work zone defined by per-direction offsets from a central anchor block.
 *
 * <p>
 * Offsets are inclusive non-negative block counts (north/south/east/west on the horizontal plane, up/down on the
 * vertical). The compact constructor rejects negative values so downstream code can rely on the invariant when
 * iterating {@link #blocksInZone()} or evaluating {@link #contains(BlockPos)}.
 *
 * <p>
 * Unlike {@link com.akikazu.colony.api.building.AxisAlignedOuterZone}, which represents an inclusive {@code [min, max]}
 * range tied to a hut block's outer bounding box, this zone is parameterized by its center so the anchor block can move
 * with the zone (e.g. if anchored to a block entity whose position changes). Persistence stores the offsets verbatim;
 * the {@link #center} is re-supplied by the owning anchor at decode time.
 */
public record AxisAlignedZone(
        BlockPos center,
        int north,
        int south,
        int east,
        int west,
        int up,
        int down)
{

    public static final Codec<AxisAlignedZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("center").forGetter(AxisAlignedZone::center),
            Codec.INT.fieldOf("north").forGetter(AxisAlignedZone::north),
            Codec.INT.fieldOf("south").forGetter(AxisAlignedZone::south),
            Codec.INT.fieldOf("east").forGetter(AxisAlignedZone::east),
            Codec.INT.fieldOf("west").forGetter(AxisAlignedZone::west),
            Codec.INT.fieldOf("up").forGetter(AxisAlignedZone::up),
            Codec.INT.fieldOf("down").forGetter(AxisAlignedZone::down))
            .apply(instance, AxisAlignedZone::new));

    public AxisAlignedZone
    {
        Objects.requireNonNull(center, "center");

        if (north < 0 || south < 0 || east < 0 || west < 0 || up < 0 || down < 0)
        {
            throw new IllegalArgumentException(
                    "AxisAlignedZone offsets must be non-negative, got "
                            + "N=" + north + " S=" + south + " E=" + east + " W=" + west
                            + " U=" + up + " D=" + down);
        }
    }

    public AxisAlignedZone withCenter(BlockPos newCenter)
    {
        Objects.requireNonNull(newCenter, "newCenter");

        return new AxisAlignedZone(newCenter, north, south, east, west, up, down);
    }

    public boolean contains(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();

        if (dx > east || -dx > west)
        {
            return false;
        }

        if (dz > south || -dz > north)
        {
            return false;
        }

        if (dy > up || -dy > down)
        {
            return false;
        }

        return true;
    }

    public int volume()
    {
        return (east + west + 1) * (up + down + 1) * (north + south + 1);
    }

    public Iterable<BlockPos> blocksInZone()
    {
        BlockPos min = new BlockPos(
                center.getX() - west,
                center.getY() - down,
                center.getZ() - north);
        BlockPos max = new BlockPos(
                center.getX() + east,
                center.getY() + up,
                center.getZ() + south);

        return BlockPos.betweenClosed(min, max);
    }
}
