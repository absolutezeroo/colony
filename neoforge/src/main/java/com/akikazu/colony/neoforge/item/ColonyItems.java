package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry holder for Colony's items. Owned by {@code :neoforge} because {@link DeferredRegister} is loader-specific.
 *
 * <p>
 * Order matters: the Town Hall {@link BlockItem} is registered before the Colony Tool so that the creative tab in
 * {@link ColonyCreativeTabs} can list the buildable first.
 */
public final class ColonyItems
{
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ColonyMod.MOD_ID);

    public static final DeferredItem<BlockItem> TOWN_HALL = ITEMS.registerSimpleBlockItem(ColonyBlocks.TOWN_HALL);

    public static final DeferredItem<ColonyToolItem> COLONY_TOOL = ITEMS.register(
            "colony_tool",
            () -> new ColonyToolItem(new Item.Properties().stacksTo(1)));

    private ColonyItems()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
