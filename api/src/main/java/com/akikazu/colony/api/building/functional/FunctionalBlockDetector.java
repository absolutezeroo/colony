package com.akikazu.colony.api.building.functional;

import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import org.jetbrains.annotations.ApiStatus;

/**
 * Maps a vanilla or modded block in the world to a Colony "function" identifier (e.g. {@code colony:bed},
 * {@code colony:door}, {@code colony:window}). Detectors are data-driven via the {@code functional_block_detector/}
 * JSON folder and registered at bootstrap; each variant's {@link #codec()} provides the {@link MapCodec} that decodes
 * its JSON payload.
 *
 * <p>
 * One detector handles one function — multiple detectors can target the same function if different block sets share it
 * (e.g. vanilla beds and modded plush beds both produce {@code colony:bed}).
 */
@ApiStatus.NonExtendable
public interface FunctionalBlockDetector
{
    Identifier id();

    Identifier detects();

    boolean matches(BlockInWorld block);

    MapCodec<? extends FunctionalBlockDetector> codec();
}
