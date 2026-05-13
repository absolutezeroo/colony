package com.akikazu.colony.core.registry;

import java.util.Objects;

/**
 * Typed handle naming a registry. Pairs an {@link Identifier} with a phantom element type {@code T} so that registry
 * consumers can be statically typed.
 */
@SuppressWarnings("UnusedTypeParameter")
public record RegistryKey<T>(Identifier id)
{
    public RegistryKey
    {
        Objects.requireNonNull(id, "id");
    }

    public static <T> RegistryKey<T> of(Identifier id)
    {
        return new RegistryKey<>(id);
    }
}
