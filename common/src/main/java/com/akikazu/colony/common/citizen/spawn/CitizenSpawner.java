package com.akikazu.colony.common.citizen.spawn;

import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Server-side spawner for the founding-cohort citizens of a colony.
 *
 * <p>
 * Spawning runs in two phases. Phase 1 (synchronous, on the founding tick): for each requested citizen, try up to
 * {@link CitizenSpawnConfig#CITIZEN_SPAWN_RETRY_ATTEMPTS} random offsets around the Town Hall. Phase 2 (asynchronous,
 * driven by {@link #tickPending(ServerLevel)} once per server tick): any citizen that did not find a valid cell in
 * phase 1 is re-attempted every {@link CitizenSpawnConfig#CITIZEN_SPAWN_RETRY_TICK_INTERVAL} ticks until either a valid
 * cell is found or {@link CitizenSpawnConfig#CITIZEN_SPAWN_RETRY_TIMEOUT_TICKS} ticks elapse — at which point the spawn
 * is dropped with an ERROR log so the founding flow does not crash a session that placed a Town Hall in a tightly
 * enclosed shell.
 */
public final class CitizenSpawner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CitizenSpawner.class);

    private static final Map<ServerLevel, Deque<PendingSpawn>> PENDING = new ConcurrentHashMap<>();

    private CitizenSpawner()
    {
    }

    public static List<EntityCitizen> spawn(
            ServerLevel level,
            BlockPos townHallPos,
            int count,
            ColonyId colonyId,
            EntityType<? extends EntityCitizen> type)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(townHallPos, "townHallPos");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(type, "type");

        if (count <= 0)
        {
            return List.of();
        }

        List<EntityCitizen> spawned = new ArrayList<>(count);

        for (int index = 0; index < count; index++)
        {
            Optional<EntityCitizen> result = attemptSpawn(level, townHallPos, colonyId, type);

            if (result.isPresent())
            {
                spawned.add(result.get());

                continue;
            }

            LOGGER.warn(
                    "Failed initial spawn attempts for colony {} near {}; queuing retry.",
                    colonyId,
                    townHallPos);
            queue(level, new PendingSpawn(townHallPos, colonyId, type, 0));
        }

        return Collections.unmodifiableList(spawned);
    }

    public static void tickPending(ServerLevel level)
    {
        Objects.requireNonNull(level, "level");
        Deque<PendingSpawn> queue = PENDING.get(level);

        if (queue == null || queue.isEmpty())
        {
            return;
        }

        List<PendingSpawn> requeue = new ArrayList<>();
        Iterator<PendingSpawn> it = queue.iterator();

        while (it.hasNext())
        {
            PendingSpawn pending = it.next();
            it.remove();
            int waited = pending.ticksWaited() + 1;

            if (waited >= CitizenSpawnConfig.CITIZEN_SPAWN_RETRY_TIMEOUT_TICKS)
            {
                LOGGER.error(
                        "Giving up on citizen spawn for colony {} near {} after {} ticks.",
                        pending.colonyId(),
                        pending.townHallPos(),
                        waited);

                continue;
            }

            if (waited % CitizenSpawnConfig.CITIZEN_SPAWN_RETRY_TICK_INTERVAL == 0)
            {
                Optional<EntityCitizen> result = attemptSpawn(
                        level,
                        pending.townHallPos(),
                        pending.colonyId(),
                        pending.type());

                if (result.isPresent())
                {
                    continue;
                }
            }

            requeue.add(new PendingSpawn(pending.townHallPos(), pending.colonyId(), pending.type(), waited));
        }

        for (PendingSpawn p : requeue)
        {
            queue.add(p);
        }
    }

    public static int pendingCount(ServerLevel level)
    {
        Deque<PendingSpawn> queue = PENDING.get(level);

        return queue == null ? 0 : queue.size();
    }

    public static void clearPending(ServerLevel level)
    {
        PENDING.remove(level);
    }

    public static boolean isValidSpawn(ServerLevel level, BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {
            return false;
        }

        BlockState at = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        return isValidSpawn(at, above, below);
    }

    public static boolean isValidSpawn(BlockState atPos, BlockState abovePos, BlockState belowPos)
    {
        Objects.requireNonNull(atPos, "atPos");
        Objects.requireNonNull(abovePos, "abovePos");
        Objects.requireNonNull(belowPos, "belowPos");

        boolean atPassable = atPos.isAir() || !atPos.blocksMotion();
        boolean aboveAir = abovePos.isAir();
        boolean belowSolid = belowPos.blocksMotion();

        return isValidSpawnCells(atPassable, aboveAir, belowSolid);
    }

    public static boolean isValidSpawnCells(boolean atPassable, boolean aboveAir, boolean belowSolid)
    {
        if (!atPassable)
        {
            return false;
        }

        if (!aboveAir)
        {
            return false;
        }

        if (!belowSolid)
        {
            return false;
        }

        return true;
    }

    private static Optional<EntityCitizen> attemptSpawn(
            ServerLevel level,
            BlockPos townHallPos,
            ColonyId colonyId,
            EntityType<? extends EntityCitizen> type)
    {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < CitizenSpawnConfig.CITIZEN_SPAWN_RETRY_ATTEMPTS; attempt++)
        {
            BlockPos candidate = pickCandidate(level, townHallPos, random);

            if (candidate == null)
            {
                continue;
            }

            EntityCitizen entity = type.create(level);

            if (entity == null)
            {
                LOGGER.error("EntityType {} produced null citizen; aborting spawn attempt.", type);

                return Optional.empty();
            }

            entity.moveTo(
                    candidate.getX() + 0.5D,
                    candidate.getY(),
                    candidate.getZ() + 0.5D,
                    random.nextFloat() * 360.0F,
                    0.0F);
            entity.setColony(colonyId);
            entity.setDisplayName(CitizenNamePool.randomName(random));
            entity.setCustomName(net.minecraft.network.chat.Component.literal(entity.displayName()));

            if (!level.addFreshEntity(entity))
            {
                LOGGER.warn(
                        "ServerLevel.addFreshEntity rejected citizen {} at {} for colony {}.",
                        entity.getCitizenId(),
                        candidate,
                        colonyId);

                continue;
            }

            return Optional.of(entity);
        }

        return Optional.empty();
    }

    private static @Nullable BlockPos pickCandidate(ServerLevel level, BlockPos townHallPos, RandomSource random)
    {
        int radius = CitizenSpawnConfig.CITIZEN_SPAWN_SEARCH_RADIUS;
        int vertical = CitizenSpawnConfig.CITIZEN_SPAWN_VERTICAL_SEARCH;
        int dx = random.nextInt(radius * 2 + 1) - radius;
        int dz = random.nextInt(radius * 2 + 1) - radius;

        for (int distance = 0; distance <= vertical; distance++)
        {
            BlockPos same = townHallPos.offset(dx, distance, dz);

            if (isValidSpawn(level, same))
            {
                return same;
            }

            if (distance > 0)
            {
                BlockPos mirror = townHallPos.offset(dx, -distance, dz);

                if (isValidSpawn(level, mirror))
                {
                    return mirror;
                }
            }
        }

        return null;
    }

    private static void queue(ServerLevel level, PendingSpawn pending)
    {
        PENDING.computeIfAbsent(level, ignored -> new ConcurrentLinkedDeque<>()).add(pending);
    }

    public static List<CitizenId> idsOf(List<EntityCitizen> citizens)
    {
        List<CitizenId> ids = new ArrayList<>(citizens.size());

        for (EntityCitizen citizen : citizens)
        {
            ids.add(citizen.getCitizenId());
        }

        return List.copyOf(ids);
    }

    record PendingSpawn(
            BlockPos townHallPos,
            ColonyId colonyId,
            EntityType<? extends EntityCitizen> type,
            int ticksWaited)
    {
    }
}
