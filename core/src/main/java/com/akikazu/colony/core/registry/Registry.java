package com.akikazu.colony.core.registry;

import com.mojang.serialization.Codec;

/**
 * Mutable registry of values keyed by {@link Identifier}. Entries are inserted during a build phase, then
 * {@link #freeze()} is called to seal the registry for the remainder of its lifetime.
 */
public interface Registry<T> extends RegistryView<T>
{
    Registry<T> register(Identifier id, T value);

    boolean isFrozen();

    void freeze();

    /**
     * Returns a {@link Codec} that encodes a registered value as its {@link Identifier} string and decodes by lookup.
     * Decoding fails if the identifier is not present in the registry. Encoding fails if the value is not registered.
     */
    Codec<T> byNameCodec();
}
