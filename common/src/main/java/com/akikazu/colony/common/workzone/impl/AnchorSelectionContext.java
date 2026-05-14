package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import java.util.Objects;

/**
 * Transient server-side memo for the LINK-mode workflow: which {@code (building, slotId)} a player has armed for the
 * next anchor right-click. Cleared when the link is applied or the manager evicts the entry on idle timeout.
 */
public record AnchorSelectionContext(BuildingId building, Identifier slotId, long armedAtTick)
{
    public AnchorSelectionContext
    {
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");
    }
}
