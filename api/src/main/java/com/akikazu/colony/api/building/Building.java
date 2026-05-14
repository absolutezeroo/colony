package com.akikazu.colony.api.building;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Public, read-only contract for a building observed at runtime. Implementations live in {@code :common}.
 *
 * <p>
 * A building has an immutable identity, a registered {@link HutType}, a Hut block position in the world, and the colony
 * it belongs to. Its {@link OuterZone} is populated once the player confirms the painting workflow — until then it is
 * empty.
 */
@ApiStatus.NonExtendable
public interface Building
{
    BuildingId id();

    HutType type();

    BlockPos hutBlockPos();

    ColonyId colony();

    Optional<OuterZone> outerZone();
}
