package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight directory entry for an anchor — enough to answer "where is anchor X?" and "which anchors does building Y
 * own?" without paging in the full block-entity state.
 *
 * <p>
 * The authoritative state (work zone, assigned crop, configuration data) lives on the anchor's
 * {@link net.minecraft.world.level.block.entity.BlockEntity}. The index is the position-keyed lookup so server services
 * and GUI tabs can iterate anchors without scanning every loaded chunk.
 */
public record AnchorMetadata(Identifier type, BlockPos position, Optional<BuildingId> linkedBuilding)
{

    public static final Codec<AnchorMetadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("type").forGetter(AnchorMetadata::type),
            BlockPos.CODEC.fieldOf("position").forGetter(AnchorMetadata::position),
            BuildingId.CODEC.optionalFieldOf("linkedBuilding").forGetter(AnchorMetadata::linkedBuilding))
            .apply(instance, AnchorMetadata::new));

    public AnchorMetadata
    {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(linkedBuilding, "linkedBuilding");
    }

    public AnchorMetadata withLink(Optional<BuildingId> link)
    {
        Objects.requireNonNull(link, "link");

        return new AnchorMetadata(type, position, link);
    }
}
