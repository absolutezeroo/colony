package com.akikazu.colony.neoforge.blockentity;

import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registry holder for Colony's block entities. Mirrors {@link ColonyBlocks} in shape — owned by {@code :neoforge}
 * because {@link DeferredRegister} is loader-specific.
 *
 * <p>
 * Holders are exposed as {@link Supplier} rather than {@code DeferredHolder} so the field declarations fit the
 * project's 120-column line cap — call sites only ever invoke {@link Supplier#get()}, which is identical between the
 * two views.
 */
public final class ColonyBlockEntities
{
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, ColonyMod.MOD_ID);

    public static final Supplier<BlockEntityType<ScarecrowBlockEntity>> SCARECROW = BLOCK_ENTITIES.register(
            "scarecrow",
            () -> BlockEntityType.Builder
                    .of(ScarecrowBlockEntity::new, ColonyBlocks.SCARECROW.get())
                    .build(null));

    private ColonyBlockEntities()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
