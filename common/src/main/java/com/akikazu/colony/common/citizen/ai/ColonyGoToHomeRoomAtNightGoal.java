package com.akikazu.colony.common.citizen.ai;

import com.akikazu.colony.api.building.functional.FunctionalBlock;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.common.building.functional.FunctionalBlockDetectorRegistry;
import com.akikazu.colony.common.building.room.RoomIndex;
import com.akikazu.colony.common.building.room.RoomRequirementEvaluator;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Walks the citizen back to its assigned home room when the in-game clock crosses sunset.
 *
 * <p>
 * Wired at high priority on {@link EntityCitizen#registerGoals()}; the wandering fallback runs only when this goal does
 * not {@link #canUse() activate}. The goal is intentionally deterministic — no randomness, no per-tick re-pathing — so
 * behavior is reproducible from GameTests. Bed lookup runs once at {@link #start() start()}; if the room contains a bed
 * (registered via {@link FunctionalBlockDetectorRegistry the bed detector}), the citizen walks to that bed, otherwise
 * to the lowest-Y center column of the footprint.
 */
public final class ColonyGoToHomeRoomAtNightGoal extends Goal
{
    private static final Identifier BED_FUNCTION = Identifier.of("colony", "bed");

    private static final long DAY_LENGTH_TICKS = 24000L;

    private static final long NIGHT_START_TICKS = 12000L;

    private static final double WALK_SPEED = 0.6D;

    private final EntityCitizen citizen;

    private @Nullable BlockPos target;

    public ColonyGoToHomeRoomAtNightGoal(EntityCitizen citizen)
    {
        this.citizen = citizen;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        Optional<RoomId> roomId = citizen.assignedHomeRoom();

        if (roomId.isEmpty() || !isNight())
        {
            return false;
        }

        Optional<FreeformZone> zone = lookupZone(roomId.get());

        if (zone.isEmpty())
        {
            return false;
        }

        return !zone.get().contains(citizen.blockPosition());
    }

    @Override
    public void start()
    {
        Optional<RoomId> roomId = citizen.assignedHomeRoom();

        if (roomId.isEmpty())
        {
            return;
        }

        Optional<FreeformZone> zone = lookupZone(roomId.get());

        if (zone.isEmpty())
        {
            return;
        }

        target = pickTarget(zone.get());

        if (target != null)
        {
            citizen.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, WALK_SPEED);
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!isNight() || target == null)
        {
            return false;
        }

        Optional<RoomId> roomId = citizen.assignedHomeRoom();

        if (roomId.isEmpty())
        {
            return false;
        }

        Optional<FreeformZone> zone = lookupZone(roomId.get());

        if (zone.isEmpty() || zone.get().contains(citizen.blockPosition()))
        {
            return false;
        }

        return !citizen.getNavigation().isDone();
    }

    @Override
    public void stop()
    {
        citizen.getNavigation().stop();
        target = null;
    }

    private boolean isNight()
    {
        return citizen.level().getDayTime() % DAY_LENGTH_TICKS > NIGHT_START_TICKS;
    }

    private Optional<FreeformZone> lookupZone(RoomId id)
    {
        if (!(citizen.level() instanceof ServerLevel server))
        {
            return Optional.empty();
        }

        return RoomIndex.get(server).findEntry(id).map(RoomIndex.Entry::zone);
    }

    private @Nullable BlockPos pickTarget(FreeformZone zone)
    {
        BlockPos bed = nearestBed(zone);

        return bed != null ? bed : centerOfFootprint(zone);
    }

    private @Nullable BlockPos nearestBed(FreeformZone zone)
    {
        if (!(citizen.level() instanceof ServerLevel server))
        {
            return null;
        }

        List<FunctionalBlock> hits = RoomRequirementEvaluator.scanZone(
                server,
                zone,
                FunctionalBlockDetectorRegistry.get().detectorsForFunction(BED_FUNCTION));

        if (hits.isEmpty())
        {
            return null;
        }

        BlockPos selfPos = citizen.blockPosition();
        BlockPos best = hits.get(0).position();
        double bestDist = best.distSqr(selfPos);

        for (int i = 1; i < hits.size(); i++)
        {
            BlockPos pos = hits.get(i).position();
            double dist = pos.distSqr(selfPos);

            if (dist < bestDist)
            {
                best = pos;
                bestDist = dist;
            }
        }

        return best;
    }

    private static @Nullable BlockPos centerOfFootprint(FreeformZone zone)
    {
        long sumX = 0L;
        long sumZ = 0L;
        int count = 0;

        for (FreeformZone.ColumnRef column : zone.footprint())
        {
            sumX += column.x();
            sumZ += column.z();
            count++;
        }

        if (count == 0)
        {
            return null;
        }

        return new BlockPos((int) (sumX / count), zone.bottomY(), (int) (sumZ / count));
    }
}
