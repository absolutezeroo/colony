package com.akikazu.colony.api.building;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Rectangular {@link OuterZone} defined by an inclusive {@code [min, max]} block range. Placeholder shape used by the
 * pending-placement state machine until the freeform variant lands in prompt 2.3.
 */
public record AxisAlignedOuterZone(BlockPos min, BlockPos max) implements OuterZone
{
    public AxisAlignedOuterZone
    {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
    }
}
