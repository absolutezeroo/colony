package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Persisted record for a single chest the player has typed to a storage slot.
 *
 * <p>
 * Sits inside {@link ChestTypingIndex}, which is the source of truth for "is the chest at X typed, and to what?". The
 * vanilla {@code ChestBlockEntity} stays unmodified — Colony only attaches metadata by position rather than by tag on
 * the block entity itself, so a chest broken and replaced loses its typing cleanly without orphaned state on the BE.
 */
public record TypedChest(BlockPos position, ColonyId colony, BuildingId building, Identifier slotId, Identifier role)
{

    public static final Codec<TypedChest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("position").forGetter(TypedChest::position),
            ColonyId.CODEC.fieldOf("colony").forGetter(TypedChest::colony),
            BuildingId.CODEC.fieldOf("building").forGetter(TypedChest::building),
            Identifier.CODEC.fieldOf("slotId").forGetter(TypedChest::slotId),
            Identifier.CODEC.fieldOf("role").forGetter(TypedChest::role))
            .apply(instance, TypedChest::new));

    public TypedChest
    {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(role, "role");
    }
}
