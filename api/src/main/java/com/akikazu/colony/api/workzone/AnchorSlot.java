package com.akikazu.colony.api.workzone;

import com.akikazu.colony.core.registry.Identifier;

import java.util.Objects;

/**
 * One declared anchor line on a {@link com.akikazu.colony.api.building.hut.HutType HutType}.
 *
 * <p>
 * Mirrors {@link com.akikazu.colony.api.storage.StorageSlot StorageSlot} in spirit: pairs an internal {@code slotId}
 * (e.g. {@code fields}) with the {@link WorkZoneAnchorType} identifier it accepts, plus per-tier capacity bounds. V1
 * uses three explicit fields for tier capacities — easier to read in a JSON datapack later than an opaque function
 * pointer, and good enough for the three-tier ladder defined in {@code docs/04-BUILDING-SYSTEM.md}.
 */
public record AnchorSlot(
        Identifier slotId,
        Identifier acceptedAnchorType,
        int maxAtTier1,
        int maxAtTier2,
        int maxAtTier3)
{
    public AnchorSlot
    {
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(acceptedAnchorType, "acceptedAnchorType");

        if (maxAtTier1 < 0 || maxAtTier2 < 0 || maxAtTier3 < 0)
        {
            throw new IllegalArgumentException(
                    "AnchorSlot capacities must be non-negative, got "
                            + "T1=" + maxAtTier1 + " T2=" + maxAtTier2 + " T3=" + maxAtTier3);
        }
    }

    public int maxAtTier(int tier)
    {
        return switch (tier)
        {
            case 1 -> maxAtTier1;
            case 2 -> maxAtTier2;
            case 3 -> maxAtTier3;
            default -> 0;
        };
    }
}
