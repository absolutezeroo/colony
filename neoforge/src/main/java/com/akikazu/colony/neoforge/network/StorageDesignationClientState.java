package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import org.jspecify.annotations.Nullable;

/**
 * Client-side mirror of {@link com.akikazu.colony.common.storage.impl.SlotSelectionManager} for HUD rendering and
 * Esc-to-cancel handling. Updated by {@link ActivateStorageModePayload}; cleared on Esc and on any subsequent client
 * mode change away from STORAGE.
 *
 * <p>
 * Holds plain data with no Minecraft client imports so the {@link ColonyPayloads} dispatcher can reference it from a
 * code path loaded on dedicated servers without dragging client classes onto the classpath.
 */
public final class StorageDesignationClientState
{
    private static volatile @Nullable Active current;

    private StorageDesignationClientState()
    {
    }

    public static void apply(ActivateStorageModePayload payload)
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
