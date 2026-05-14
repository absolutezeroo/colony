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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Puts the citizen in a bed inside its assigned home room overnight.
 *
 * <p>
 * Layered above {@link ColonyGoToHomeRoomAtNightGoal}: the latter brings the citizen into the room footprint, and this
 * goal takes over once the citizen is standing inside, snapping it onto the closest bed via
 * {@link net.minecraft.world.entity.LivingEntity#startSleeping LivingEntity.startSleeping}. Vanilla
 * {@code PathfinderMob} inherits {@code startSleeping}/{@code stopSleeping}/{@code isSleeping} from
 * {@code LivingEntity}, so no extra plumbing is needed here — the entity goes into
 * {@link net.minecraft.world.entity.Pose#SLEEPING} pose and the bed's {@link BedBlock#OCCUPIED} state flips
 * automatically.
 *
 * <p>
 * Fatigue recovery is intentionally not yet wired (the {@code fatigue} field lands in a follow-up prompt). Today the
 * goal logs a TRACE line each tick of sleep so the behavior can be verified without breaking the public Citizen API.
 */
public final class ColonySleepInBedGoal extends Goal
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonySleepInBedGoal.class);

    private static final Identifier BED_FUNCTION = Identifier.of("colony", "bed");

    private static final long DAY_LENGTH_TICKS = 24000L;

    private static final long NIGHT_START_TICKS = 12000L;

    private final EntityCitizen citizen;

    private @Nullable BlockPos bedPos;

    public ColonySleepInBedGoal(EntityCitizen citizen)
    {
        this.citizen = citizen;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        if (citizen.isSleeping() || !isNight())
        {
            return false;
        }

        Optional<RoomId> roomId = citizen.assignedHomeRoom();

        if (roomId.isEmpty())
        {
            return false;
        }

        Optional<FreeformZone> zone = lookupZone(roomId.get());

        if (zone.isEmpty() || !zone.get().contains(citizen.blockPosition()))
        {
            return false;
        }

        bedPos = findBedHead(zone.get());

        return bedPos != null;
    }

    @Override
    public void start()
    {
        if (bedPos == null)
        {
            return;
        }

        citizen.getNavigation().stop();
        citizen.startSleeping(bedPos);
    }

    @Override
    public boolean canContinueToUse()
    {
        return citizen.isSleeping() && isNight();
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (citizen.isSleeping() && LOGGER.isTraceEnabled())
        {
            LOGGER.trace("citizen sleeping id={} pos={}", citizen.getCitizenId(), bedPos);
        }
    }

    @Override
    public void stop()
    {
        if (citizen.isSleeping())
        {
            citizen.stopSleeping();
        }

        bedPos = null;
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

    private @Nullable BlockPos findBedHead(FreeformZone zone)
    {
        if (!(citizen.level() instanceof ServerLevel server))
        {
            return null;
        }

        List<FunctionalBlock> hits = RoomRequirementEvaluator.scanZone(
                server,
                zone,
                FunctionalBlockDetectorRegistry.get().detectorsForFunction(BED_FUNCTION));

        BlockPos selfPos = citizen.blockPosition();
        BlockPos best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (FunctionalBlock hit : hits)
        {
            BlockState state = hit.blockState();

            if (!(state.getBlock() instanceof BedBlock))
            {
                continue;
            }

            if (state.getValue(BedBlock.OCCUPIED))
            {
                continue;
            }

            double dist = hit.position().distSqr(selfPos);

            if (dist < bestDist)
            {
                best = hit.position();
                bestDist = dist;
            }
        }

        return best;
    }
}
