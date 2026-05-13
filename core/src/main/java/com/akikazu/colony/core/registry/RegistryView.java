package com.akikazu.colony.core.registry;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Read-only projection of a {@link Registry}. Exposed to consumers that should not be able to mutate registry contents.
 */
public interface RegistryView<T>
{
    RegistryKey<T> key();

    Optional<T> get(Identifier id);

    Optional<Identifier> idOf(T value);

    Stream<T> values();

    int size();
}
