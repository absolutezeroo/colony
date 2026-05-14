package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.entity.ColonyEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PathfindingGameTests
{
    private static final int PATH_TIMEOUT_TICKS = 200;

    private static final int NAV_DELAY_TICKS = 2;

    private static final double REACH_RADIUS_BLOCKS = 2.5D;

    private PathfindingGameTests()
    {
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = PATH_TIMEOUT_TICKS)
    public static void citizenPathfindsToTarget(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));

        layStoneRect(level, origin, -2, 8, -2, 8);

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        citizen.goalSelector.removeAllGoals(g -> true);

        BlockPos target = helper.absolutePos(new BlockPos(6, 2, 6));

        helper.runAfterDelay(NAV_DELAY_TICKS, () -> citizen.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.0D));

        helper.succeedWhen(() -> helper.assertTrue(
                horizontalDistance(citizen, target) < REACH_RADIUS_BLOCKS,
                "Citizen should reach target within "
                        + REACH_RADIUS_BLOCKS
                        + " blocks (current dist="
                        + horizontalDistance(citizen, target)
                        + ")"));
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = PATH_TIMEOUT_TICKS)
    public static void citizenAvoidsLavaInPath(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));

        layStoneRect(level, origin, -2, 9, -2, 7);

        for (int z = 0; z <= 4; z++)
        {
            level.setBlock(origin.offset(4, 1, z), Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
        }

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        citizen.goalSelector.removeAllGoals(g -> true);

        BlockPos target = helper.absolutePos(new BlockPos(7, 2, 1));

        float initialHealth = citizen.getHealth();

        helper.runAfterDelay(NAV_DELAY_TICKS, () -> citizen.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.0D));

        helper.succeedWhen(() -> assertReachedSafely(helper, citizen, target, initialHealth));
    }

    @GameTest(template = "empty_3x3x3", timeoutTicks = PATH_TIMEOUT_TICKS)
    public static void citizenFailsGracefullyWhenNoPath(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));

        buildEnclosedBox(level, origin);

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        citizen.goalSelector.removeAllGoals(g -> true);

        BlockPos target = helper.absolutePos(new BlockPos(20, 2, 20));
        double startX = citizen.getX();
        double startZ = citizen.getZ();

        helper.runAfterDelay(NAV_DELAY_TICKS, () -> citizen.getNavigation().moveTo(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                1.0D));

        helper.runAfterDelay(PATH_TIMEOUT_TICKS - 10, () -> assertStranded(helper, citizen, startX, startZ));
    }

    private static void layStoneRect(ServerLevel level, BlockPos origin, int xMin, int xMax, int zMin, int zMax)
    {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = xMin; x <= xMax; x++)
        {
            for (int z = zMin; z <= zMax; z++)
            {
                level.setBlock(origin.offset(x, 0, z), stone, 3);
                level.setBlock(origin.offset(x, 1, z), air, 3);
                level.setBlock(origin.offset(x, 2, z), air, 3);
                level.setBlock(origin.offset(x, 3, z), air, 3);
            }
        }
    }

    private static void buildEnclosedBox(ServerLevel level, BlockPos origin)
    {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = -1; x <= 3; x++)
        {
            for (int z = -1; z <= 3; z++)
            {
                level.setBlock(origin.offset(x, 0, z), stone, 3);
                level.setBlock(origin.offset(x, 4, z), stone, 3);
            }
        }

        for (int y = 1; y <= 3; y++)
        {
            for (int x = -1; x <= 3; x++)
            {
                level.setBlock(origin.offset(x, y, -1), stone, 3);
                level.setBlock(origin.offset(x, y, 3), stone, 3);
            }

            for (int z = -1; z <= 3; z++)
            {
                level.setBlock(origin.offset(-1, y, z), stone, 3);
                level.setBlock(origin.offset(3, y, z), stone, 3);
            }
        }

        for (int y = 1; y <= 3; y++)
        {
            for (int x = 0; x <= 2; x++)
            {
                for (int z = 0; z <= 2; z++)
                {
                    level.setBlock(origin.offset(x, y, z), air, 3);
                }
            }
        }
    }

    private static void assertReachedSafely(
            GameTestHelper helper,
            EntityCitizen citizen,
            BlockPos target,
            float initialHealth)
    {
        helper.assertTrue(
                citizen.getHealth() >= initialHealth,
                "Citizen should not have lost health to lava (initial="
                        + initialHealth
                        + ", current="
                        + citizen.getHealth()
                        + ")");

        helper.assertTrue(
                horizontalDistance(citizen, target) < REACH_RADIUS_BLOCKS,
                "Citizen should reach target around the lava wall (dist="
                        + horizontalDistance(citizen, target)
                        + ")");
    }

    private static void assertStranded(GameTestHelper helper, EntityCitizen citizen, double startX, double startZ)
    {
        helper.assertTrue(citizen.isAlive(), "Citizen should still be alive after failed pathfind");

        double dx = citizen.getX() - startX;
        double dz = citizen.getZ() - startZ;
        double distSq = (dx * dx) + (dz * dz);

        helper.assertTrue(
                distSq < (REACH_RADIUS_BLOCKS * REACH_RADIUS_BLOCKS),
                "Citizen should have stayed near spawn (drift=" + Math.sqrt(distSq) + ")");

        helper.assertTrue(
                citizen.getNavigation().isDone(),
                "Citizen navigation should be idle when no path exists");

        helper.succeed();
    }

    private static double horizontalDistance(EntityCitizen citizen, BlockPos target)
    {
        double dx = citizen.getX() - (target.getX() + 0.5D);
        double dz = citizen.getZ() - (target.getZ() + 0.5D);

        return Math.sqrt((dx * dx) + (dz * dz));
    }
}
