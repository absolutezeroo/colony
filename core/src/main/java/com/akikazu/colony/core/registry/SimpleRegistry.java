package com.akikazu.colony.core.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default {@link Registry} implementation backed by hash maps. Mutable until {@link #freeze()} is invoked. After freeze
 * the registry is effectively immutable; visibility to other threads is established by the volatile {@code frozen} flag
 * combined with the final backing fields.
 */
public final class SimpleRegistry<T> implements Registry<T>
{
    private final RegistryKey<T> key;
    private final Map<Identifier, T> byId = new HashMap<>();
    private final IdentityHashMap<T, Identifier> toId = new IdentityHashMap<>();
    private volatile boolean frozen;

    public SimpleRegistry(RegistryKey<T> key)
    {
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public RegistryKey<T> key()
    {
        return key;
    }

    @Override
    public Registry<T> register(Identifier id, T value)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");

        if (frozen)
        {
            throw new IllegalStateException(
                    "Registry %s is frozen; cannot register %s".formatted(key.id(), id));
        }

        if (byId.containsKey(id))
        {
            throw new IllegalStateException(
                    "Registry %s already contains %s".formatted(key.id(), id));
        }

        byId.put(id, value);
        toId.put(value, id);

        return this;
    }

    @Override
    public Optional<T> get(Identifier id)
    {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Identifier> idOf(T value)
    {
        return Optional.ofNullable(toId.get(value));
    }

    @Override
    public Stream<T> values()
    {
        return byId.values().stream();
    }

    @Override
    public int size()
    {
        return byId.size();
    }

    @Override
    public boolean isFrozen()
    {
        return frozen;
    }

    @Override
    public void freeze()
    {
        frozen = true;
    }

    @Override
    public Codec<T> byNameCodec()
    {
        return Identifier.CODEC.flatXmap(
                id -> Optional.ofNullable(byId.get(id))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(
                                () -> "Unknown %s entry: %s".formatted(key.id(), id))),
                value -> Optional.ofNullable(toId.get(value))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(
                                () -> "Value not registered in %s: %s".formatted(key.id(), value))));
    }
}
