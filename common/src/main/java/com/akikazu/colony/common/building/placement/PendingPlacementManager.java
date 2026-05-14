package com.akikazu.colony.common.building.placement;

import com.akikazu.colony.api.building.hut.HutType;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side state tracker for in-progress hut placements. One {@link PendingPlacement} per player at most; starting a
 * new placement while one is active replaces it (the player switched targets).
 *
 * <p>
 * The manager is intentionally in-memory: pendings are NOT persisted across server restart, and a player who
 * disconnects mid-painting loses their pending. See {@link PendingPlacement} for the rationale.
 */
public final class PendingPlacementManager
{
    private final Map<UUID, PendingPlacement> activePlacements = new ConcurrentHashMap<>();

    public PendingPlacement start(UUID player, HutType type, BlockPos pos, ResourceKey<Level> dim, long currentTick)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(dim, "dim");

        PendingPlacement pending = new PendingPlacement(
                player,
                type,
                pos.immutable(),
                dim,
                currentTick,
                PendingPlacementState.AWAITING_PAINTING);

        activePlacements.put(player, pending);

        return pending;
    }

    public Optional<PendingPlacement> get(UUID player)
    {
        Objects.requireNonNull(player, "player");

        return Optional.ofNullable(activePlacements.get(player));
    }

    public Optional<PendingPlacement> cancel(UUID player)
    {
        Objects.requireNonNull(player, "player");

        return Optional.ofNullable(activePlacements.remove(player));
    }

    /**
     * Marks the pending as confirmed and removes it. Actual zone validation and block placement land in prompt 2.3 —
     * this method exists so the C2S confirm payload has somewhere to dispatch and tests can assert the transition.
     */
    public Optional<PendingPlacement> confirm(UUID player)
    {
        Objects.requireNonNull(player, "player");

        return Optional.ofNullable(activePlacements.remove(player));
    }

    public void clearAll()
    {
        activePlacements.clear();
    }
}
