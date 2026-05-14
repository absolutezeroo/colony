package com.akikazu.colony.neoforge.network;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Client-only holder for the two corners painted during a pending hut placement.
 *
 * <p>
 * Each right-click on a block while the Colony Tool is in zone mode is routed through
 * {@link #recordRightClick(BlockPos)}, which alternates between writing corner A and corner B so the player can
 * progressively refine the zone. Cleared whenever the pending placement itself is cleared. Like
 * {@link PendingPlacementClientState} this class deliberately avoids Minecraft client imports so it can be referenced
 * from server-only dispatcher code without dragging client classes onto a dedicated server's classpath.
 */
public final class ZonePaintingClientState
{
    private static volatile @Nullable BlockPos cornerA;

    private static volatile @Nullable BlockPos cornerB;

    private static volatile Corner lastSet = Corner.NONE;

    public enum Corner
    {
        NONE, A, B
    }

    private ZonePaintingClientState()
    {
    }

    /**
     * Records a right-click in the painting workflow. Alternates between corner A and corner B so the player can keep
     * right-clicking to refine either corner; the AABB between the two latest positions is always the zone.
     */
    public static void recordRightClick(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        if (lastSet == Corner.A)
        {
            cornerB = pos.immutable();
            lastSet = Corner.B;
        }
        else
        {
            cornerA = pos.immutable();
            lastSet = Corner.A;
        }
    }

    public static Optional<BlockPos> cornerA()
    {
        return Optional.ofNullable(cornerA);
    }

    public static Optional<BlockPos> cornerB()
    {
        return Optional.ofNullable(cornerB);
    }

    public static Corner lastSet()
    {
        return lastSet;
    }

    public static boolean hasBothCorners()
    {
        return cornerA != null && cornerB != null;
    }

    public static void clear()
    {
        cornerA = null;
        cornerB = null;
        lastSet = Corner.NONE;
    }
}
