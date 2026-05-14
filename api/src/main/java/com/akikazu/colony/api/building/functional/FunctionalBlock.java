package com.akikazu.colony.api.building.functional;

import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * The result of a successful {@link FunctionalBlockDetector#matches detector match}: a concrete block in a zone tagged
 * with the function the detector identifies. Consumed by room requirement evaluation, quality scoring (V2), and tier
 * evaluation passes.
 */
public record FunctionalBlock(Identifier function, BlockPos position, BlockState blockState)
{
    public FunctionalBlock
    {
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(blockState, "blockState");

        position = position.immutable();
    }
}
