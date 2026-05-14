package com.akikazu.colony.api.storage;

import com.akikazu.colony.core.registry.Identifier;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * One declared storage line on a {@link com.akikazu.colony.api.building.hut.HutType HutType}.
 *
 * <p>
 * A slot binds an internal id ({@code seeds_input}, {@code harvest_output}) to a {@link StorageRoleType} via
 * {@link #acceptedRole()}, with optional extra item filtering on top of the role's own filter. The number of chests the
 * slot can hold grows with the building's tier — exposed through {@link #maxChestsAtTier(int)} rather than a flat cap
 * so future tiers can scale capacity without changing the slot declaration.
 */
public record StorageSlot(
        Identifier slotId,
        Identifier acceptedRole,
        IntUnaryOperator tierCapacity,
        Optional<ItemFilter> additionalFilter)
{
    public StorageSlot
    {
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(acceptedRole, "acceptedRole");
        Objects.requireNonNull(tierCapacity, "tierCapacity");
        Objects.requireNonNull(additionalFilter, "additionalFilter");
    }

    public int maxChestsAtTier(int tier)
    {
        return tierCapacity.applyAsInt(tier);
    }

    public static StorageSlot withConstantCapacity(
            Identifier slotId,
            Identifier acceptedRole,
            int capacity)
    {
        return new StorageSlot(slotId, acceptedRole, ignored -> capacity, Optional.empty());
    }

    public static StorageSlot withConstantCapacity(
            Identifier slotId,
            Identifier acceptedRole,
            int capacity,
            ItemFilter additionalFilter)
    {
        Objects.requireNonNull(additionalFilter, "additionalFilter");

        return new StorageSlot(slotId, acceptedRole, ignored -> capacity, Optional.of(additionalFilter));
    }
}
