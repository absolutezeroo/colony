package com.akikazu.colony.common.storage.filter;

import com.akikazu.colony.api.storage.ItemFilter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ItemFilter} that accepts every non-empty {@link ItemStack}. Used by the V1 {@code general} role and as a
 * permissive default for roles whose narrowing happens at the slot level instead of the role level.
 */
public final class AnyItemFilter implements ItemFilter
{
    public static final AnyItemFilter INSTANCE = new AnyItemFilter();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_filter.any");

    private AnyItemFilter()
    {
    }

    @Override
    public boolean matches(ItemStack stack)
    {
        return !stack.isEmpty();
    }

    @Override
    public Component displayName()
    {
        return DISPLAY_NAME;
    }
}
