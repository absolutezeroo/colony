package com.akikazu.colony.common.storage.filter;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaggedItemFilterTest
{
    private static final TagKey<Item> TEST_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("colony", "test_tag"));

    @Test
    void tagAccessorReturnsConstructorArgument()
    {
        TaggedItemFilter filter = new TaggedItemFilter(TEST_TAG);

        assertEquals(TEST_TAG, filter.tag());
    }

    @Test
    void displayNameIsNotNull()
    {
        TaggedItemFilter filter = new TaggedItemFilter(TEST_TAG);

        assertNotNull(filter.displayName());
    }

    @Test
    @SuppressWarnings("NullAway")
    void nullTagRejected()
    {
        assertThrows(NullPointerException.class, () -> new TaggedItemFilter(null));
    }
}
