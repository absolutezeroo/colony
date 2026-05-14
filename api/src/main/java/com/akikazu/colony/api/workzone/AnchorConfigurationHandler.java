package com.akikazu.colony.api.workzone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Per-{@link WorkZoneAnchorType anchor-type} hook for right-click configuration with an item.
 *
 * <p>
 * The dispatch layer calls {@link #accepts(ItemStack)} to decide whether the item is a configuration input for this
 * anchor (vs. a vanilla pass-through). When accepted, {@link #apply(ItemStack, CompoundTag)} mutates the supplied
 * {@code anchorData} tag (which is the block entity's persisted configuration blob) and returns the user-facing
 * feedback alongside whether the player's stack should shrink by one.
 *
 * <p>
 * Implementations live in {@code :common} alongside their anchor type. They never touch level state directly — only the
 * tag passed in — so they can be unit-tested without a server.
 */
public interface AnchorConfigurationHandler
{
    boolean accepts(ItemStack stack);

    AnchorConfigurationResult apply(ItemStack stack, CompoundTag anchorData);
}
