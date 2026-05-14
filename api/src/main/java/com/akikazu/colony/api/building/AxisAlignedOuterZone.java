package com.akikazu.colony.api.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Rectangular {@link OuterZone} defined by an inclusive {@code [min, max]} block range.
 *
 * <p>
 * Constructed directly when both corners are already normalized (e.g. on codec decode), or via
 * {@link #fromCorners(BlockPos, BlockPos)} when the caller has two unsorted clicks from the painting workflow. The
 * compact constructor asserts {@code min <= max} on every axis so downstream code can rely on the invariant without
 * defensive checks.
 */
public record AxisAlignedOuterZone(BlockPos min, BlockPos max) implements OuterZone
{
    public static final Codec<AxisAlignedOuterZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("min").forGetter(AxisAlignedOuterZone::min),
            BlockPos.CODEC.fieldOf("max").forGetter(AxisAlignedOuterZone::max))
            .apply(instance, AxisAlignedOuterZone::new));

    public AxisAlignedOuterZone
    {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ())
        {
            throw new IllegalArgumentException(
                    "AxisAlignedOuterZone requires min <= max on every axis, got min=" + min + " max=" + max);
        }
    }

    public static AxisAlignedOuterZone fromCorners(BlockPos a, BlockPos b)
    {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        BlockPos normalizedMin = new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ()));
        BlockPos normalizedMax = new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ()));

        return new AxisAlignedOuterZone(normalizedMin, normalizedMax);
    }

    public boolean contains(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public int volume()
    {
        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }

    public boolean overlaps(AxisAlignedOuterZone other)
    {
        Objects.requireNonNull(other, "other");

        return min.getX() <= other.max.getX() && max.getX() >= other.min.getX()
                && min.getY() <= other.max.getY() && max.getY() >= other.min.getY()
                && min.getZ() <= other.max.getZ() && max.getZ() >= other.min.getZ();
    }

    public Iterable<BlockPos> blocksInZone()
    {
        return BlockPos.betweenClosed(min, max);
    }
}
