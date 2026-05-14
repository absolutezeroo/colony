package com.akikazu.colony.common.storage.filter;

import com.akikazu.colony.api.storage.ItemFilter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Disjunctive composition of {@link ItemFilter}s: matches when any delegate matches.
 *
 * <p>
 * Empty delegate lists are rejected at construction so the filter never silently denies every stack — a use case the
 * codebase has no demand for and that would be much harder to debug than the early failure.
 */
public record CompositeItemFilter(List<ItemFilter> delegates) implements ItemFilter
{
    public CompositeItemFilter
    {
        Objects.requireNonNull(delegates, "delegates");

        if (delegates.isEmpty())
        {
            throw new IllegalArgumentException("CompositeItemFilter requires at least one delegate");
        }

        delegates = List.copyOf(delegates);
    }

    @Override
    public boolean matches(ItemStack stack)
    {
        for (ItemFilter filter : delegates)
        {
            if (filter.matches(stack))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Component displayName()
    {
        String joined = delegates.stream()
                .map(filter -> filter.displayName().getString())
                .collect(Collectors.joining(" / "));

        return Component.translatable("colony.storage_filter.composite", joined);
    }
}
