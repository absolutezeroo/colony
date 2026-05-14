package com.akikazu.colony.common.building;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Always-loaded summary of a building, sufficient to answer "which colony owns this zone?" and "where is each Hut
 * block?" without paging in the full per-colony snapshot.
 *
 * <p>
 * Stored in {@link BuildingIndex} (mirrors the colony / {@link com.akikazu.colony.common.colony.ColonyMetadata} pair).
 * The {@link HutType} is encoded by its registered {@link Identifier} and re-resolved at decode time via
 * {@link ColonyBootstrap#hutTypesView()}; if the type identifier is unknown on load, decoding fails fast rather than
 * silently dropping the building.
 */
public record BuildingMetadata(ColonyId colony, HutType hutType, BlockPos hutPos, AxisAlignedOuterZone outerZone)
{

    public static final Codec<BuildingMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ColonyId.CODEC.fieldOf("colony").forGetter(BuildingMetadata::colony),
            Identifier.CODEC.fieldOf("hutType").forGetter(m -> m.hutType().id()),
            BlockPos.CODEC.fieldOf("hutPos").forGetter(BuildingMetadata::hutPos),
            AxisAlignedOuterZone.CODEC.fieldOf("outerZone").forGetter(BuildingMetadata::outerZone))
            .apply(instance, BuildingMetadata::fromCodec));

    public BuildingMetadata
    {
        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(hutType, "hutType");
        Objects.requireNonNull(hutPos, "hutPos");
        Objects.requireNonNull(outerZone, "outerZone");
    }

    private static BuildingMetadata fromCodec(
            ColonyId colony,
            Identifier hutTypeId,
            BlockPos hutPos,
            AxisAlignedOuterZone outerZone)
    {
        HutType type = ColonyBootstrap.hutTypesView()
                .get(hutTypeId)
                .orElseThrow(() -> new IllegalStateException("Unknown HutType identifier on decode: " + hutTypeId));

        return new BuildingMetadata(colony, type, hutPos, outerZone);
    }
}
