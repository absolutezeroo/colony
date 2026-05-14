package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player armed anchor-link selections — mirrors
 * {@link com.akikazu.colony.common.storage.impl.SlotSelectionManager} for the LINK workflow.
 *
 * <p>
 * Idle timeout matches the storage manager (5 in-game minutes / 6000 ticks). Eviction is lazy on {@link #current}
 * lookup so the manager doesn't run a timer thread for a short-lived UX feature.
 */
public final class AnchorSelectionManager
{
    public static final long IDLE_TIMEOUT_TICKS = 6000L;

    private final Map<UUID, AnchorSelectionContext> selections = new ConcurrentHashMap<>();

    public void arm(UUID player, BuildingId building, Identifier slotId, long currentTick)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");

        selections.put(player, new AnchorSelectionContext(building, slotId, currentTick));
    }

    public Optional<AnchorSelectionContext> current(UUID player, long currentTick)
    {
        Objects.requireNonNull(player, "player");

        AnchorSelectionContext ctx = selections.get(player);

        if (ctx == null)
        {
            return Optional.empty();
        }

        if (currentTick - ctx.armedAtTick() > IDLE_TIMEOUT_TICKS)
        {
            selections.remove(player, ctx);

            return Optional.empty();
        }

        return Optional.of(ctx);
    }

    public void clear(UUID player)
    {
        Objects.requireNonNull(player, "player");

        selections.remove(player);
    }

    public int size()
    {
        return selections.size();
    }
}
