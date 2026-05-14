package com.akikazu.colony.common.workzone.scarecrow;

import com.akikazu.colony.api.workzone.AnchorConfigurationHandler;
import com.akikazu.colony.api.workzone.AnchorConfigurationResult;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Translates a player's right-click with a seed-like item into a crop assignment on a scarecrow.
 *
 * <p>
 * Acceptance is gated by
 * {@link com.akikazu.colony.common.workzone.scarecrow.ScarecrowAnchorType#configurationItemTag()} so the datapack
 * drives which items qualify. On {@link #apply}, the item is mapped to its in-world crop block: a potato item maps to
 * {@link Blocks#POTATOES}, wheat seeds map to {@link Blocks#WHEAT}, and so on. The resulting block identifier is
 * written to {@code anchorData} under {@link #ASSIGNED_CROP_KEY}.
 *
 * <p>
 * The handler does not touch level state — only the supplied tag. Side effects (chat feedback, decrement) are produced
 * by the dispatch layer based on the returned {@link AnchorConfigurationResult}.
 */
public final class ScarecrowConfigurationHandler implements AnchorConfigurationHandler
{
    public static final String ASSIGNED_CROP_KEY = "assignedCrop";

    public static final TagKey<Item> COMPATIBLE_CROPS = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("colony", "scarecrow_compatible_crops"));

    public static final ScarecrowConfigurationHandler INSTANCE = new ScarecrowConfigurationHandler();

    private static volatile @Nullable Map<Item, Block> itemToCrop;

    private static Map<Item, Block> itemToCrop()
    {
        Map<Item, Block> cached = itemToCrop;

        if (cached != null)
        {
            return cached;
        }

        Map<Item, Block> built = Map.of(
                Items.WHEAT_SEEDS, Blocks.WHEAT,
                Items.POTATO, Blocks.POTATOES,
                Items.CARROT, Blocks.CARROTS,
                Items.BEETROOT_SEEDS, Blocks.BEETROOTS,
                Items.PUMPKIN_SEEDS, Blocks.PUMPKIN_STEM,
                Items.MELON_SEEDS, Blocks.MELON_STEM,
                Items.NETHER_WART, Blocks.NETHER_WART);

        itemToCrop = built;

        return built;
    }

    private ScarecrowConfigurationHandler()
    {
    }

    @Override
    public boolean accepts(ItemStack stack)
    {
        Objects.requireNonNull(stack, "stack");

        if (stack.isEmpty())
        {
            return false;
        }

        return stack.is(COMPATIBLE_CROPS) || itemToCrop().containsKey(stack.getItem());
    }

    @Override
    public AnchorConfigurationResult apply(ItemStack stack, CompoundTag anchorData)
    {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(anchorData, "anchorData");

        if (!accepts(stack))
        {
            return new AnchorConfigurationResult.Rejected(
                    Component.translatable("colony.message.scarecrow.invalid_item"));
        }

        Block crop = resolveCropBlock(stack);

        if (crop == null)
        {
            return new AnchorConfigurationResult.Rejected(
                    Component.translatable("colony.message.scarecrow.invalid_item"));
        }

        ResourceLocation cropId = BuiltInRegistries.BLOCK.getKey(crop);
        anchorData.putString(ASSIGNED_CROP_KEY, cropId.toString());

        Component cropName = Component.translatable(crop.getDescriptionId());

        return new AnchorConfigurationResult.Applied(
                Component.translatable("colony.message.scarecrow.assigned_crop", cropName),
                true);
    }

    private static @Nullable Block resolveCropBlock(ItemStack stack)
    {
        Item item = stack.getItem();
        Block direct = itemToCrop().get(item);

        if (direct != null)
        {
            return direct;
        }

        if (stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", "seeds"))))
        {
            return Blocks.WHEAT;
        }

        if (item instanceof BlockItem blockItem)
        {
            return blockItem.getBlock();
        }

        return null;
    }
}
