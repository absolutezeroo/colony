package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.core.registry.Identifier;

/**
 * Outcome of an {@link AnchorLinkingService} call. Sealed so chat-formatting and gametest assertions exhaustively
 * switch over the three variants; the failure path collapses into a flat {@link Reason} enum so callers stay terse.
 */
public sealed interface AnchorLinkingResult
{
    record Linked(AnchorId anchor, BuildingId building, Identifier slotId) implements AnchorLinkingResult
    {
    }

    record Unlinked(AnchorId anchor) implements AnchorLinkingResult
    {
    }

    record Rejected(Reason reason) implements AnchorLinkingResult
    {
    }

    enum Reason
    {
        NO_ANCHOR, NO_BUILDING, WRONG_TYPE, OUTSIDE_RANGE, CAPACITY_FULL, ALREADY_LINKED, UNKNOWN_SLOT
    }
}
