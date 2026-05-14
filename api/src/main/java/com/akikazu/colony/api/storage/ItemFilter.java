package com.akikazu.colony.api.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Predicate over an {@link ItemStack}. Used by {@link StorageRoleType#filter()} and by per-slot overrides on
 * {@link StorageSlot#additionalFilter()} to gate which stacks may enter a typed chest.
 *
 * <p>
 * Implementations are deliberately stateless: the same filter instance is queried for every stack the storage layer
 * vets, and they must remain safe to call from the server's main tick thread.
 */
public interface ItemFilter
{
    boolean matches(ItemStack stack);

    Component displayName();
}
