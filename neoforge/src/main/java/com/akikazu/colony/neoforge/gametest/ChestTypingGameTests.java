package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.storage.impl.ChestTypingIndex;
import com.akikazu.colony.common.storage.impl.ChestTypingResult;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Reason;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Rejected;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Success;
import com.akikazu.colony.common.storage.impl.ChestTypingService;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.gametest.fixture.TestHutTypeWithStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Covers the prompt 2.4 chest-typing service end-to-end against a live {@link ServerLevel} and {@link BuildingIndex}.
 *
 * <p>
 * Each test seeds a {@link TestHutTypeWithStorage}-backed building covering the test platform, places (or does not
 * place) a chest, drives {@link ChestTypingService#assignChest} directly, and asserts the typed result. The fixture
 * HutType stays out of {@code ColonyBootstrap} so it never lands on a real save.
 */
@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChestTypingGameTests
{
    private ChestTypingGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    public static void typingChestInsideBuildingSucceeds(GameTestHelper helper)
    {
        Fixture fx = setup(helper, 3);
        BlockPos chest = fx.placeChest(1, 2, 1);

        ChestTypingResult result = ChestTypingService.assignChest(
                fx.level, fx.player, chest, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        if (!(result instanceof Success success))
        {
            helper.fail("Expected Success, got " + result);

            return;
        }

        if (!success.typed().slotId().equals(TestHutTypeWithStorage.SLOT_INPUT))
        {
            helper.fail("Typed chest slot mismatch: " + success.typed().slotId());

            return;
        }

        if (ChestTypingIndex.get(fx.level).findAt(chest).isEmpty())
        {
            helper.fail("ChestTypingIndex should contain the chest after assignment");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void typingChestOutsideBuildingFails(GameTestHelper helper)
    {
        Fixture fx = setup(helper, 3);
        BlockPos far = helper.absolutePos(new BlockPos(50, 2, 50));
        fx.level.setBlock(far, Blocks.CHEST.defaultBlockState(), 3);

        ChestTypingResult result = ChestTypingService.assignChest(
                fx.level, fx.player, far, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        assertRejected(helper, result, Reason.OUTSIDE_BUILDING);
    }

    @GameTest(template = "empty_3x3_platform")
    public static void typingNonChestBlockFails(GameTestHelper helper)
    {
        Fixture fx = setup(helper, 3);
        BlockPos stone = helper.absolutePos(new BlockPos(1, 2, 1));
        fx.level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);

        ChestTypingResult result = ChestTypingService.assignChest(
                fx.level, fx.player, stone, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        assertRejected(helper, result, Reason.NOT_A_CHEST);
    }

    @GameTest(template = "empty_3x3_platform")
    public static void typingFailsAtCapacity(GameTestHelper helper)
    {
        Fixture fx = setup(helper, 1);
        BlockPos first = fx.placeChest(0, 2, 0);
        BlockPos second = fx.placeChest(2, 2, 2);

        ChestTypingService.assignChest(fx.level, fx.player, first, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        ChestTypingResult result = ChestTypingService.assignChest(
                fx.level, fx.player, second, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        assertRejected(helper, result, Reason.CAPACITY_FULL);
    }

    @GameTest(template = "empty_3x3_platform")
    public static void typingAlreadyTypedChestFails(GameTestHelper helper)
    {
        Fixture fx = setupWithSecondBuilding(helper, 3);
        BlockPos chest = fx.placeChest(1, 2, 1);

        ChestTypingService.assignChest(fx.level, fx.player, chest, fx.buildingId, TestHutTypeWithStorage.SLOT_INPUT);

        ChestTypingResult result = ChestTypingService.assignChest(
                fx.level, fx.player, chest, fx.otherBuildingId, TestHutTypeWithStorage.SLOT_INPUT);

        assertRejected(helper, result, Reason.ALREADY_TYPED);
    }

    private static void assertRejected(GameTestHelper helper, ChestTypingResult result, Reason expected)
    {
        if (!(result instanceof Rejected rejected))
        {
            helper.fail("Expected Rejected(" + expected + "), got " + result);

            return;
        }

        if (rejected.reason() != expected)
        {
            helper.fail("Expected reason " + expected + ", got " + rejected.reason());

            return;
        }

        helper.succeed();
    }

    private static Fixture setup(GameTestHelper helper, int capacity)
    {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        ChestTypingIndex.get(level).entries().keySet().forEach(pos -> ChestTypingIndex.get(level).clear(pos));

        TestHutTypeWithStorage hutType = new TestHutTypeWithStorage(capacity);

        BlockPos hutPos = helper.absolutePos(new BlockPos(1, 2, 1));
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(
                helper.absolutePos(new BlockPos(0, 1, 0)),
                helper.absolutePos(new BlockPos(2, 4, 2)));
        BuildingId buildingId = BuildingId.random();
        BuildingMetadata metadata = new BuildingMetadata(ColonyId.random(), hutType, hutPos, zone);

        BuildingIndex.get(level).register(buildingId, metadata);

        return new Fixture(helper, level, player, buildingId, BuildingId.random());
    }

    private static Fixture setupWithSecondBuilding(GameTestHelper helper, int capacity)
    {
        Fixture base = setup(helper, capacity);

        TestHutTypeWithStorage otherHutType = new TestHutTypeWithStorage(capacity);
        AxisAlignedOuterZone otherZone = new AxisAlignedOuterZone(
                helper.absolutePos(new BlockPos(30, 1, 30)),
                helper.absolutePos(new BlockPos(32, 4, 32)));
        BuildingMetadata otherMetadata = new BuildingMetadata(
                ColonyId.random(),
                otherHutType,
                helper.absolutePos(new BlockPos(31, 2, 31)),
                otherZone);

        BuildingIndex.get(base.level).register(base.otherBuildingId, otherMetadata);

        return base;
    }

    private static final class Fixture
    {
        private final GameTestHelper helper;

        private final ServerLevel level;

        private final ServerPlayer player;

        private final BuildingId buildingId;

        private final BuildingId otherBuildingId;

        private Fixture(
                GameTestHelper helper,
                ServerLevel level,
                ServerPlayer player,
                BuildingId buildingId,
                BuildingId otherBuildingId)
        {
            this.helper = helper;
            this.level = level;
            this.player = player;
            this.buildingId = buildingId;
            this.otherBuildingId = otherBuildingId;
        }

        BlockPos placeChest(int x, int y, int z)
        {
            BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);

            return pos;
        }
    }
}
