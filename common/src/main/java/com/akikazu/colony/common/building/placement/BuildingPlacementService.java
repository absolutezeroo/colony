package com.akikazu.colony.common.building.placement;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.BuildingEvents;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.building.BuildingImpl;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.building.validation.ZoneValidationResult;
import com.akikazu.colony.common.building.validation.ZoneValidator;
import com.akikazu.colony.common.colony.ColonyEventBus;
import com.akikazu.colony.common.colony.ColonyIndex;
import com.akikazu.colony.common.colony.ColonyMetadata;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side orchestrator for the moment the player presses Enter on a painted zone.
 *
 * <p>
 * Splits the workflow into deterministic steps so {@code ColonyPayloads} (in {@code :neoforge}) can call
 * {@link #attemptPlacement} from a payload handler and GameTests can call it from a synthetic context. The loader
 * adapter passes the {@link BlockState} to place — the service is loader-agnostic; the actual block class lives in
 * {@code :neoforge}.
 */
public final class BuildingPlacementService
{
    private final ServerLevel level;

    private BuildingPlacementService(ServerLevel level)
    {
        this.level = Objects.requireNonNull(level, "level");
    }

    public static BuildingPlacementService get(ServerLevel level)
    {
        return new BuildingPlacementService(level);
    }

    public Result attemptPlacement(
            UUID playerUuid,
            PendingPlacement pending,
            BlockPos cornerA,
            BlockPos cornerB,
            Block hutBlock)
    {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(pending, "pending");
        Objects.requireNonNull(cornerA, "cornerA");
        Objects.requireNonNull(cornerB, "cornerB");
        Objects.requireNonNull(hutBlock, "hutBlock");

        AxisAlignedOuterZone zone = AxisAlignedOuterZone.fromCorners(cornerA, cornerB);

        ColonyId colony = resolveColony()
                .orElse(new ColonyId(new UUID(0L, 0L)));

        ZoneValidationResult validation = ZoneValidator.validate(level, zone, pending.targetHutPos(), colony);

        if (validation instanceof ZoneValidationResult.Invalid invalid)
        {
            return new Result.Invalid(invalid);
        }

        AxisAlignedOuterZone accepted = ((ZoneValidationResult.Valid) validation).zone();

        BlockState placed = hutBlock.defaultBlockState();
        level.setBlock(pending.targetHutPos(), placed, Block.UPDATE_ALL);

        HutType hutType = pending.hutType();
        BuildingId id = BuildingId.random();
        BuildingImpl building = new BuildingImpl(id, hutType, pending.targetHutPos(), colony, accepted);

        BuildingIndex index = BuildingIndex.get(level);

        if (!index.register(id, new BuildingMetadata(colony, hutType, pending.targetHutPos(), accepted)))
        {
            throw new IllegalStateException("BuildingId collision on " + id);
        }

        ColonyEventBus.get().post(new BuildingEvents.BuildingCreatedEvent(building, playerUuid));

        return new Result.Valid(building);
    }

    private Optional<ColonyId> resolveColony()
    {
        ColonyIndex index = ColonyIndex.get(level);

        for (Map.Entry<ColonyId, ColonyMetadata> entry : index.entries().entrySet())
        {
            if (!entry.getValue().dimension().equals(level.dimension()))
            {
                continue;
            }

            return Optional.of(entry.getKey());
        }

        return Optional.empty();
    }

    public sealed interface Result
    {
        record Valid(BuildingImpl building) implements Result
        {
        }

        record Invalid(ZoneValidationResult.Invalid validation) implements Result
        {
        }
    }
}
