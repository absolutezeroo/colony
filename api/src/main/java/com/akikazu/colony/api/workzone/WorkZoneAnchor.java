package com.akikazu.colony.api.workzone;

import com.akikazu.colony.api.building.BuildingId;

import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Live view of a placed anchor in the world. Implementations are owned by the loader tier (typically a
 * {@code BlockEntity}) which authors the {@link AxisAlignedZone} and persists the link to its building.
 *
 * <p>
 * The interface is read-only on purpose: mutation paths (linking, zone-edit) flow through dedicated server-side
 * services that update the underlying block entity and any associated index.
 */
public interface WorkZoneAnchor
{
    AnchorId id();

    WorkZoneAnchorType type();

    BlockPos position();

    Optional<BuildingId> linkedBuilding();

    AxisAlignedZone workZone();
}
