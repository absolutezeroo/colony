package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player active chest-typing slot selections.
 *
 * <p>
 * V1 spec calls for a 5-minute idle timeout (6000 server ticks at 20 t/s); the manager evicts stale entries lazily on
 * {@link #current} lookup so we don't spin a timer thread for a feature most players use seconds at a time. Held by
 * {@code ColonyServerSession} so the map's lifetime matches the {@link net.minecraft.server.MinecraftServer} instance.
 */
public final class SlotSelectionManager
{
    public static final long IDLE_TIMEOUT_TICKS = 6000L;

    private final Map<UUID, SlotSelectionContext> selections = new ConcurrentHashMap<>();

    public void arm(UUID player, BuildingId building, Identifier slotId, long currentTick)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");

        selections.put(player, new SlotSelectionContext(building, slotId, currentTick));
    }

    public Optional<SlotSelectionContext> current(UUID player, long currentTick)
    {
        Objects.requireNonNull(player, "player");

        SlotSelectionContext ctx = selections.get(player);

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
