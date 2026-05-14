package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import org.jspecify.annotations.Nullable;

/**
 * Client-side mirror of {@link com.akikazu.colony.common.workzone.impl.AnchorSelectionManager} for HUD rendering and
 * Esc-to-cancel handling — sibling to {@link StorageDesignationClientState}. Updated by
 * {@link ActivateLinkModePayload}; cleared on Esc and on any subsequent mode change away from LINK.
 */
public final class AnchorLinkingClientState
{
    private static volatile @Nullable Active current;

    private AnchorLinkingClientState()
    {
    }

    public static void apply(ActivateLinkModePayload payload)
    {
        current = new Active(payload.building(), payload.slotId());
    }

    public static @Nullable Active current()
    {
        return current;
    }

    public static void clear()
    {
        current = null;
    }

    public record Active(BuildingId building, Identifier slotId)
    {
    }
}
