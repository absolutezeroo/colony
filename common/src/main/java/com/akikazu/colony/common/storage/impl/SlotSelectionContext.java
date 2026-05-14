package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import java.util.Objects;

/**
 * Transient server-side memo: which slot of which building a player has currently armed for chest typing.
 *
 * <p>
 * Set when the player clicks "Designate Chests" in the Building Storage tab (or, in V1, an equivalent command); cleared
 * after the chest is assigned or after the manager's idle timeout. {@code armedAtTick} is the server tick at which the
 * selection was made; {@link SlotSelectionManager} compares it against the current tick to evict stale entries instead
 * of running a timer thread.
 */
public record SlotSelectionContext(BuildingId building, Identifier slotId, long armedAtTick)
{
    public SlotSelectionContext
    {
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");
    }
}
