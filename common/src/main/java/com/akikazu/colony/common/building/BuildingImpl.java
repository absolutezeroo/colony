package com.akikazu.colony.common.building;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.Building;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.OuterZone;
import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link Building} implementation. Immutable; constructed by the placement workflow once
 * {@link com.akikazu.colony.common.building.validation.ZoneValidator} accepts the painted zone, then registered in
 * {@link BuildingIndex}.
 */
public record BuildingImpl(BuildingId id, HutType type, BlockPos hutBlockPos, ColonyId colony,
        AxisAlignedOuterZone zone)
        implements Building
{
    public BuildingImpl
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(hutBlockPos, "hutBlockPos");
        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(zone, "zone");
    }

    @Override
    public Optional<OuterZone> outerZone()
    {
        return Optional.of(zone);
    }

    public BuildingMetadata toMetadata()
    {
        return new BuildingMetadata(colony, type, hutBlockPos, zone);
    }
}
