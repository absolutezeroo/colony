package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.FreeformZone.ColumnRef;
import com.akikazu.colony.api.building.room.Room;
import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.common.building.room.BedroomType;
import com.akikazu.colony.common.building.room.RoomIndex;
import com.akikazu.colony.common.building.room.RoomValidator;
import com.akikazu.colony.common.citizen.assignment.CitizenAssignmentServiceImpl;
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GameTests for the home-room sleep behavior wired by {@code ColonyGoToHomeRoomAtNightGoal} and
 * {@code ColonySleepInBedGoal}.
 *
 * <p>
 * Each test builds a small bedroom with one bed inside the GameTest platform, registers the room with
 * {@link RoomIndex}, spawns a citizen, assigns the room via {@link CitizenAssignmentServiceImpl}, and then drives the
 * world clock to verify the goal switches on/off as expected.
 */
@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CitizenSleepGameTests
{
    private static final int GO_HOME_TIMEOUT_TICKS = 400;

    private static final int SLEEP_TIMEOUT_TICKS = 600;

    private static final int WAKE_TIMEOUT_TICKS = 200;

    private static final long NIGHT_TIME = 13000L;

    private static final long DAY_TIME = 1000L;

    private CitizenSleepGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform", timeoutTicks = GO_HOME_TIMEOUT_TICKS)
    public static void citizenWithAssignedRoomGoesToRoomAtNight(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        layStoneFloor(helper);
        BlockPos bedHead = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos bedFoot = helper.absolutePos(new BlockPos(1, 2, 2));
        placeBed(level, bedHead, bedFoot);

        FreeformZone zone = zoneCovering(helper, 0, 0, 2, 2);
        Room room = RoomValidator.confirm(level, BuildingId.random(), BedroomType.INSTANCE, zone);
        RoomId roomId = RoomIndex.get(level).register(room);

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(0, 2, 0));
        citizen.setAssignedHomeRoom(roomId);
        CitizenAssignmentServiceImpl.get(level).assignHomeRoom(citizen.getCitizenId(), roomId);

        level.setDayTime(NIGHT_TIME);

        helper.succeedWhen(() -> helper.assertTrue(
                zone.contains(citizen.blockPosition()),
                "Citizen should have walked into the home room at night (pos=" + citizen.blockPosition() + ")"));
    }

    @GameTest(template = "empty_3x3_platform", timeoutTicks = SLEEP_TIMEOUT_TICKS)
    public static void citizenSleepsInBedAtNight(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        layStoneFloor(helper);
        BlockPos bedHead = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos bedFoot = helper.absolutePos(new BlockPos(1, 2, 2));
        placeBed(level, bedHead, bedFoot);

        FreeformZone zone = zoneCovering(helper, 0, 0, 2, 2);
        Room room = RoomValidator.confirm(level, BuildingId.random(), BedroomType.INSTANCE, zone);
        RoomId roomId = RoomIndex.get(level).register(room);

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        citizen.setAssignedHomeRoom(roomId);
        CitizenAssignmentServiceImpl.get(level).assignHomeRoom(citizen.getCitizenId(), roomId);

        level.setDayTime(NIGHT_TIME);

        helper.succeedWhen(() -> helper.assertTrue(
                citizen.isSleeping(),
                "Citizen should be sleeping by now (sleeping=" + citizen.isSleeping() + ")"));
    }

    @GameTest(template = "empty_3x3_platform", timeoutTicks = WAKE_TIMEOUT_TICKS)
    public static void citizenLeavesBedAtDay(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        layStoneFloor(helper);
        BlockPos bedHead = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos bedFoot = helper.absolutePos(new BlockPos(1, 2, 2));
        placeBed(level, bedHead, bedFoot);

        FreeformZone zone = zoneCovering(helper, 0, 0, 2, 2);
        Room room = RoomValidator.confirm(level, BuildingId.random(), BedroomType.INSTANCE, zone);
        RoomId roomId = RoomIndex.get(level).register(room);

        EntityCitizen citizen = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        citizen.setAssignedHomeRoom(roomId);
        CitizenAssignmentServiceImpl.get(level).assignHomeRoom(citizen.getCitizenId(), roomId);

        citizen.startSleeping(bedHead);
        level.setDayTime(DAY_TIME);

        helper.succeedWhen(() -> helper.assertTrue(
                !citizen.isSleeping(),
                "Citizen should have left the bed once it became day (sleeping=" + citizen.isSleeping() + ")"));
    }

    private static FreeformZone zoneCovering(GameTestHelper helper, int x0, int z0, int x1, int z1)
    {
        Set<ColumnRef> footprint = new LinkedHashSet<>();

        for (int x = x0; x <= x1; x++)
        {
            for (int z = z0; z <= z1; z++)
            {
                BlockPos absolute = helper.absolutePos(new BlockPos(x, 0, z));
                footprint.add(new ColumnRef(absolute.getX(), absolute.getZ()));
            }
        }

        int bottomY = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        int topY = helper.absolutePos(new BlockPos(0, 4, 0)).getY();

        return FreeformZone.of(footprint, bottomY, topY);
    }

    private static void layStoneFloor(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x <= 2; x++)
        {
            for (int z = 0; z <= 2; z++)
            {
                level.setBlock(helper.absolutePos(new BlockPos(x, 1, z)), stone, 3);
                level.setBlock(helper.absolutePos(new BlockPos(x, 2, z)), air, 3);
                level.setBlock(helper.absolutePos(new BlockPos(x, 3, z)), air, 3);
            }
        }
    }

    private static void placeBed(ServerLevel level, BlockPos head, BlockPos foot)
    {
        level.setBlock(head, Blocks.RED_BED.defaultBlockState(), 3);
        level.setBlock(foot, Blocks.RED_BED.defaultBlockState(), 3);
    }
}
