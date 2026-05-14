package com.akikazu.colony.common.building.placement;

import com.akikazu.colony.api.building.hut.HutType;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of an in-progress hut placement attempt. Tracked server-side by {@link PendingPlacementManager} between the
 * moment the player right-clicks with a Hut block item and the moment they confirm or cancel the painting workflow.
 *
 * <p>
 * In-memory only — pendings are NOT persisted across server restart. If the owning player disconnects mid-painting the
 * record is lost; we may add persistence later but it is non-critical for V1.
 */
public record PendingPlacement(
        UUID playerUuid,
        HutType hutType,
        BlockPos targetHutPos,
        ResourceKey<Level> dimension,
        long startedAtTick,
        PendingPlacementState state)
{
    public PendingPlacement
    {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(hutType, "hutType");
        Objects.requireNonNull(targetHutPos, "targetHutPos");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(state, "state");
    }

    public PendingPlacement withState(PendingPlacementState newState)
    {
        return new PendingPlacement(playerUuid, hutType, targetHutPos, dimension, startedAtTick, newState);
    }
}
