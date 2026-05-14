package com.akikazu.colony.common.storage.filter;

import com.akikazu.colony.api.storage.ItemFilter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeItemFilterTest
{
    @Test
    void emptyDelegatesRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> new CompositeItemFilter(List.of()));
    }

    @Test
    void delegatesAreCopied()
    {
        ItemFilter dummy = new DummyFilter();
        List<ItemFilter> mutable = new ArrayList<>();
        mutable.add(dummy);

        CompositeItemFilter filter = new CompositeItemFilter(mutable);
        mutable.clear();

        assertEquals(1, filter.delegates().size());
    }

    @Test
    void singleDelegateRetained()
    {
        ItemFilter dummy = new DummyFilter();
        CompositeItemFilter filter = new CompositeItemFilter(List.of(dummy));

        assertEquals(1, filter.delegates().size());
        assertEquals(dummy, filter.delegates().get(0));
    }

    @Test
    void multipleDelegatesPreserveOrder()
    {
        ItemFilter a = new DummyFilter();
        ItemFilter b = new DummyFilter();
        ItemFilter c = new DummyFilter();

        CompositeItemFilter filter = new CompositeItemFilter(List.of(a, b, c));

        assertEquals(3, filter.delegates().size());
        assertEquals(a, filter.delegates().get(0));
        assertEquals(b, filter.delegates().get(1));
        assertEquals(c, filter.delegates().get(2));
    }

    private static final class DummyFilter implements ItemFilter
    {
        @Override
        public boolean matches(net.minecraft.world.item.ItemStack stack)
        {
            return false;
        }

        @Override
        public net.minecraft.network.chat.Component displayName()
        {
            return net.minecraft.network.chat.Component.translatable("test.dummy");
        }
    }
}
