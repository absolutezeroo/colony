package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.FreeformZone.ColumnRef;
import com.akikazu.colony.api.building.room.Room;
import com.akikazu.colony.api.building.room.RoomStatus;
import com.akikazu.colony.common.building.room.BedroomType;
import com.akikazu.colony.common.building.room.RoomValidator;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GameTests for the binary V1 room requirement pass: a bedroom with one or two beds passes; zero or three beds fails.
 *
 * <p>
 * The tests bypass the painting workflow and call {@link RoomValidator#confirm} directly with a freshly built
 * {@link FreeformZone}. This is the same surface a future GUI confirmation packet handler will call, so behavior is
 * exercised end-to-end through the evaluator.
 */
@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RoomRequirementGameTests
{
    private RoomRequirementGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    public static void bedroomWithBedIsValid(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos head = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos foot = helper.absolutePos(new BlockPos(1, 2, 2));

        placeBed(helper, head, foot);

        Room room = RoomValidator.confirm(
                level,
                BuildingId.random(),
                BedroomType.INSTANCE,
                zoneCovering(helper, 0, 0, 2, 2));

        if (!(room.status() instanceof RoomStatus.Valid))
        {
            helper.fail("Expected Valid status, got " + room.status());

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void bedroomWithoutBedIsInvalid(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        Room room = RoomValidator.confirm(
                level,
                BuildingId.random(),
                BedroomType.INSTANCE,
                zoneCovering(helper, 0, 0, 2, 2));

        if (!(room.status() instanceof RoomStatus.Invalid invalid))
        {
            helper.fail("Expected Invalid status, got " + room.status());

            return;
        }

        if (invalid.errors().isEmpty())
        {
            helper.fail("Invalid status must carry at least one error");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void bedroomWithThreeBedsIsInvalid(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        placeBed(helper, helper.absolutePos(new BlockPos(0, 2, 0)), helper.absolutePos(new BlockPos(0, 2, 1)));
        placeBed(helper, helper.absolutePos(new BlockPos(1, 2, 0)), helper.absolutePos(new BlockPos(1, 2, 1)));
        placeBed(helper, helper.absolutePos(new BlockPos(2, 2, 0)), helper.absolutePos(new BlockPos(2, 2, 1)));

        Room room = RoomValidator.confirm(
                level,
                BuildingId.random(),
                BedroomType.INSTANCE,
                zoneCovering(helper, 0, 0, 2, 2));

        if (!(room.status() instanceof RoomStatus.Invalid invalid))
        {
            helper.fail("Expected Invalid status (too many beds), got " + room.status());

            return;
        }

        boolean mentionsMax = invalid.errors().stream()
                .anyMatch(e -> e.reason().contains("above max"));

        if (!mentionsMax)
        {
            helper.fail("Invalid status must reference the max-beds violation, got " + invalid.errors());

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void reevaluateRefreshesStatusAfterBedPlaced(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();

        Room initial = RoomValidator.confirm(
                level,
                BuildingId.random(),
                BedroomType.INSTANCE,
                zoneCovering(helper, 0, 0, 2, 2));

        if (!(initial.status() instanceof RoomStatus.Invalid))
        {
            helper.fail("Initial bedroom (no beds) should be Invalid, got " + initial.status());

            return;
        }

        placeBed(helper, helper.absolutePos(new BlockPos(1, 2, 1)), helper.absolutePos(new BlockPos(1, 2, 2)));

        Room refreshed = RoomValidator.reevaluate(level, initial);

        if (!(refreshed.status() instanceof RoomStatus.Valid))
        {
            helper.fail("After bed placed, reevaluate must return Valid, got " + refreshed.status());

            return;
        }

        if (!refreshed.id().equals(initial.id()))
        {
            helper.fail("reevaluate must preserve the room identity");

            return;
        }

        helper.succeed();
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

    private static void placeBed(GameTestHelper helper, BlockPos head, BlockPos foot)
    {
        ServerLevel level = helper.getLevel();
        level.setBlock(head, Blocks.RED_BED.defaultBlockState(), 3);
        level.setBlock(foot, Blocks.RED_BED.defaultBlockState(), 3);
    }
}
