package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.common.citizen.spawn.CitizenSpawnConfig;
import com.akikazu.colony.common.citizen.spawn.CitizenSpawner;
import com.akikazu.colony.common.colony.ColonyIndex;
import com.akikazu.colony.common.colony.ColonyManager;
import com.akikazu.colony.common.colony.ColonyMetadata;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.event.TownHallPlacementListener;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TownHallFoundingGameTests
{
    private static final int FOUNDING_TIMEOUT_TICKS = 200;

    private static final int RETRY_TIMEOUT_TICKS = 110;

    private static final double CITIZEN_COUNT_RADIUS = 12.0D;

    private TownHallFoundingGameTests()
    {
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = FOUNDING_TIMEOUT_TICKS)
    public static void placingTownHallFoundsColonyAndSpawns4Citizens(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos townHall = helper.absolutePos(new BlockPos(1, 2, 1));

        layStoneFloor(level, townHall, 7);

        UUID founder = UUID.randomUUID();
        ColonyId colonyId = TownHallPlacementListener.foundColonyAt(level, townHall, founder, "TestColony");

        helper.succeedWhen(() -> assertFoundingPopulation(helper, level, townHall, colonyId));
    }

    @GameTest(template = "empty_3x3x3")
    public static void colonyPersistsAcrossSaveReload(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos townHall = helper.absolutePos(new BlockPos(1, 2, 1));

        UUID founder = UUID.randomUUID();
        ColonyId colonyId = TownHallPlacementListener.foundColonyAt(level, townHall, founder, "PersistColony");

        ColonyIndex live = ColonyIndex.get(level);
        CompoundTag saved = live.save(new CompoundTag(), level.registryAccess());
        ColonyIndex reloaded = ColonyIndex.factory().deserializer().apply(saved, level.registryAccess());

        if (!reloaded.contains(colonyId))
        {
            helper.fail("Reloaded index missing colony id " + colonyId);

            return;
        }

        Optional<ColonyMetadata> reloadedMeta = reloaded.find(colonyId);

        if (reloadedMeta.isEmpty())
        {
            helper.fail("Reloaded index missing metadata for " + colonyId);

            return;
        }

        if (!townHall.equals(reloadedMeta.get().townHallPos()))
        {
            helper.fail("Town hall position was not preserved across reload");

            return;
        }

        if (ColonyManager.get(level).loadFull(colonyId).isEmpty())
        {
            helper.fail("Per-colony snapshot file missing after founding for " + colonyId);

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = RETRY_TIMEOUT_TICKS)
    public static void citizenSpawnRetriesIfSpaceUnavailable(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos townHall = helper.absolutePos(new BlockPos(1, 2, 1));

        fillSpawnRegionSolid(level, townHall);
        CitizenSpawner.clearPending(level);

        UUID founder = UUID.randomUUID();
        ColonyId colonyId = TownHallPlacementListener.foundColonyAt(level, townHall, founder, "BlockedColony");

        helper.runAfterDelay(
                CitizenSpawnConfig.CITIZEN_SPAWN_RETRY_TIMEOUT_TICKS + 5,
                () -> assertSpawnsDroppedAfterTimeout(helper, level, townHall, colonyId));
    }

    private static void assertFoundingPopulation(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos townHall,
            ColonyId colonyId)
    {
        List<EntityCitizen> citizens = citizensOfColony(level, townHall, CITIZEN_COUNT_RADIUS, colonyId);

        helper.assertTrue(
                citizens.size() >= CitizenSpawnConfig.STARTING_CITIZEN_COUNT,
                "Expected at least " + CitizenSpawnConfig.STARTING_CITIZEN_COUNT
                        + " citizens spawned, saw " + citizens.size());

        for (EntityCitizen citizen : citizens)
        {
            Optional<ColonyId> affiliated = citizen.colony();
            helper.assertTrue(
                    affiliated.isPresent() && affiliated.get().equals(colonyId),
                    "Citizen " + citizen.getCitizenId() + " is not affiliated with founded colony " + colonyId);
        }

        ColonyManager manager = ColonyManager.get(level);
        helper.assertTrue(
                manager.find(colonyId).isPresent(),
                "Founded colony id " + colonyId + " missing from index");
    }

    private static void assertSpawnsDroppedAfterTimeout(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos townHall,
            ColonyId colonyId)
    {
        List<EntityCitizen> citizens = citizensOfColony(level, townHall, CITIZEN_COUNT_RADIUS, colonyId);

        helper.assertTrue(
                citizens.size() < CitizenSpawnConfig.STARTING_CITIZEN_COUNT,
                "Expected fewer than " + CitizenSpawnConfig.STARTING_CITIZEN_COUNT
                        + " citizens to spawn in a blocked region, saw " + citizens.size());

        helper.assertTrue(
                ColonyManager.get(level).find(colonyId).isPresent(),
                "Colony index entry should persist even when spawns fail");

        helper.assertTrue(
                CitizenSpawner.pendingCount(level) == 0,
                "Pending spawn queue should drain after the retry timeout");

        helper.succeed();
    }

    private static void layStoneFloor(ServerLevel level, BlockPos center, int radius)
    {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                level.setBlock(center.offset(dx, -1, dz), stone, 3);
                level.setBlock(center.offset(dx, 2, dz), stone, 3);
                level.setBlock(center.offset(dx, 0, dz), air, 3);
                level.setBlock(center.offset(dx, 1, dz), air, 3);
                level.setBlock(center.offset(dx, 3, dz), air, 3);
                level.setBlock(center.offset(dx, 4, dz), air, 3);
            }
        }
    }

    private static List<EntityCitizen> citizensOfColony(
            ServerLevel level,
            BlockPos center,
            double radius,
            ColonyId colonyId)
    {
        AABB box = new AABB(
                center.getX() - radius,
                center.getY() - radius,
                center.getZ() - radius,
                center.getX() + radius,
                center.getY() + radius,
                center.getZ() + radius);

        return level.getEntitiesOfClass(
                EntityCitizen.class,
                box,
                citizen -> citizen.colony().filter(id -> id.equals(colonyId)).isPresent());
    }

    private static void fillSpawnRegionSolid(ServerLevel level, BlockPos center)
    {
        BlockState stone = Blocks.STONE.defaultBlockState();
        int radius = CitizenSpawnConfig.CITIZEN_SPAWN_SEARCH_RADIUS;
        int vertical = CitizenSpawnConfig.CITIZEN_SPAWN_VERTICAL_SEARCH;

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = -vertical; dy <= vertical; dy++)
                {
                    level.setBlock(center.offset(dx, dy, dz), stone, 3);
                }
            }
        }
    }
}
