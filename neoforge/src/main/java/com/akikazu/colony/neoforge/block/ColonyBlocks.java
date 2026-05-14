package com.akikazu.colony.neoforge.block;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry holder for Colony's blocks. Owned by {@code :neoforge} because {@link DeferredRegister} is loader-specific.
 *
 * <p>
 * The Town Hall is intentionally cheap to place in V1 (wood + a single gold ingot) so single-player founding feels
 * frictionless. Tier-gating and economy costs are tracked in {@code docs/11-ECONOMY-V1-V2.md}.
 */
public final class ColonyBlocks
{
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ColonyMod.MOD_ID);

    public static final DeferredHolder<Block, TownHallBlock> TOWN_HALL = BLOCKS.register(
            "town_hall",
            () -> new TownHallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F)
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Block, ResidenceHutBlock> RESIDENCE_HUT = BLOCKS.register(
            "residence_hut",
            () -> new ResidenceHutBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F)
                    .sound(SoundType.WOOD)));

    private ColonyBlocks()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
    }
}
