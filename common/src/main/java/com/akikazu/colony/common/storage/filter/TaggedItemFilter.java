package com.akikazu.colony.common.storage.filter;

import com.akikazu.colony.api.storage.ItemFilter;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * {@link ItemFilter} backed by an item {@link TagKey}. Matches stacks whose item belongs to the tag at the time of the
 * check, which means datapack overrides take effect on the next reload without code changes.
 */
public record TaggedItemFilter(TagKey<Item> tag) implements ItemFilter
{
    public TaggedItemFilter
    {
        Objects.requireNonNull(tag, "tag");
    }

    @Override
    public boolean matches(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(tag);
    }

    @Override
    public Component displayName()
    {
        return Component.translatable("colony.storage_filter.tag", "#" + tag.location());
    }
}
