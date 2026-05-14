package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative-mode tab grouping every Colony item so the player can find the Town Hall and Colony Tool without browsing
 * vanilla tabs.
 */
public final class ColonyCreativeTabs
{
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, ColonyMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COLONY = TABS.register(
            "colony",
            ColonyCreativeTabs::buildColonyTab);

    private static CreativeModeTab buildColonyTab()
    {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.colony"))
                .icon(() -> new ItemStack(ColonyItems.TOWN_HALL.get()))
                .displayItems(ColonyCreativeTabs::fillColonyTab)
                .build();
    }

    private static void fillColonyTab(
            CreativeModeTab.ItemDisplayParameters parameters,
            CreativeModeTab.Output output)
    {
        output.accept(ColonyItems.TOWN_HALL.get());
        output.accept(ColonyItems.RESIDENCE_HUT.get());
        output.accept(ColonyItems.SCARECROW.get());
        output.accept(ColonyItems.COLONY_TOOL.get());
    }

    private ColonyCreativeTabs()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        TABS.register(modEventBus);
    }

    public static ResourceLocation tabId()
    {
        return ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "colony");
    }
}
