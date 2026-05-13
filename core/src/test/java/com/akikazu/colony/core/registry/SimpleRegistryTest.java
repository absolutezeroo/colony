package com.akikazu.colony.core.registry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRegistryTest
{
    private static final RegistryKey<String> KEY = RegistryKey.of(Identifier.of("colony", "test"));

    @Test
    void register_then_get_round_trips_value()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);
        Identifier id = Identifier.of("colony", "alpha");

        registry.register(id, "first");

        assertEquals(Optional.of("first"), registry.get(id));
        assertEquals(1, registry.size());
    }

    @Test
    void idOf_returns_id_for_registered_value()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);
        Identifier id = Identifier.of("colony", "beta");
        String value = "second";

        registry.register(id, value);

        assertEquals(Optional.of(id), registry.idOf(value));
    }

    @Test
    void idOf_returns_empty_for_unknown_value()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);

        assertTrue(registry.idOf("not-registered").isEmpty());
    }

    @Test
    void get_returns_empty_for_unknown_id()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);

        assertTrue(registry.get(Identifier.of("colony", "missing")).isEmpty());
    }

    @Test
    void freeze_blocks_subsequent_register()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);
        registry.register(Identifier.of("colony", "alpha"), "first");

        assertFalse(registry.isFrozen());

        registry.freeze();

        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class,
                () -> registry.register(Identifier.of("colony", "beta"), "second"));
    }

    @Test
    void register_rejects_duplicate_identifier()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);
        Identifier id = Identifier.of("colony", "alpha");
        registry.register(id, "first");

        assertThrows(IllegalStateException.class, () -> registry.register(id, "second"));
    }

    @Test
    void register_returns_self_for_chaining()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);

        Registry<String> returned = registry.register(Identifier.of("colony", "alpha"), "first");

        assertSame(registry, returned);
    }

    @Test
    void values_streams_all_registered_entries()
    {
        SimpleRegistry<String> registry = new SimpleRegistry<>(KEY);
        registry.register(Identifier.of("colony", "alpha"), "first");
        registry.register(Identifier.of("colony", "beta"), "second");

        long count = registry.values().count();

        assertEquals(2, count);
    }
}
