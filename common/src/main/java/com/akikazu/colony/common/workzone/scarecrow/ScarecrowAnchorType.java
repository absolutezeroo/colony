package com.akikazu.colony.common.workzone.scarecrow;

import com.akikazu.colony.api.workzone.AnchorConfigurationHandler;
import com.akikazu.colony.api.workzone.WorkZoneAnchorType;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Built-in {@link WorkZoneAnchorType} for the scarecrow, registered under {@code colony:anchor/scarecrow}.
 *
 * <p>
 * The {@link #anchorBlock()} resolution looks up the block by identifier instead of holding a direct reference: the
 * block is registered in {@code :neoforge} via {@code DeferredRegister}, but this type lives in {@code :common} and
 * cannot import the loader tier. By the time {@code anchorBlock()} is called (gameplay, not mod construction), the
 * block registry has been populated, so the lookup is safe.
 */
public final class ScarecrowAnchorType implements WorkZoneAnchorType
{
    public static final Identifier ID = Identifier.of("colony", "anchor/scarecrow");

    public static final ResourceLocation BLOCK_ID = ResourceLocation.fromNamespaceAndPath("colony", "scarecrow");

    public static final ScarecrowAnchorType INSTANCE = new ScarecrowAnchorType();

    private static final Component DISPLAY_NAME = Component.translatable("anchor.colony.scarecrow");

    private ScarecrowAnchorType()
    {
    }

    @Override
    public Identifier id()
    {
        return ID;
    }

    @Override
    public Component displayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public Block anchorBlock()
    {
        return BuiltInRegistries.BLOCK.get(BLOCK_ID);
    }

    @Override
    public TagKey<Item> configurationItemTag()
    {
        return ScarecrowConfigurationHandler.COMPATIBLE_CROPS;
    }

    @Override
    public AnchorConfigurationHandler configurationHandler()
    {
        return ScarecrowConfigurationHandler.INSTANCE;
    }
}
