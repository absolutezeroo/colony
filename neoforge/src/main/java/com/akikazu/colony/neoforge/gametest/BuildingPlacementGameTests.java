package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.building.impl.ResidenceHutType;
import com.akikazu.colony.common.building.placement.BuildingPlacementService;
import com.akikazu.colony.common.building.placement.PendingPlacement;
import com.akikazu.colony.common.building.placement.PendingPlacementManager;
import com.akikazu.colony.common.building.placement.PendingPlacementState;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;
import com.akikazu.colony.neoforge.block.event.TownHallPlacementListener;
import com.akikazu.colony.neoforge.network.ColonyServerSession;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * End-to-end coverage for the prompt 2.3 placement workflow: zone validation, building registration, error rejection.
 *
 * <p>
 * The tests don't run the client-side painting steps — that is exercised by mod-loaded interactive play. They drive the
 * server-side {@link BuildingPlacementService} directly, which is exactly what the
 * {@link com.akikazu.colony.neoforge.network.ConfirmZonePaintingPayload} handler does. Each test founds its own colony
 * so {@link BuildingPlacementService} has a colony to attach the building to.
 */
@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BuildingPlacementGameTests
{
    private BuildingPlacementGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    public static void paintingValidZoneCreatesBuilding(GameTestHelper helper)
    {
        Context ctx = setup(helper);
        BlockPos hutPos = helper.absolutePos(new BlockPos(1, 2, 1));
        startPending(ctx, hutPos);

        BlockPos cornerA = hutPos.offset(-2, -1, -2);
        BlockPos cornerB = hutPos.offset(2, 2, 2);

        BuildingPlacementService.Result result = BuildingPlacementService.get(ctx.level)
                .attemptPlacement(ctx.playerUuid, ctx.pending(), cornerA, cornerB, ColonyBlocks.RESIDENCE_HUT.get());

        if (!(result instanceof BuildingPlacementService.Result.Valid valid))
        {
            helper.fail("Expected Valid result, got " + result);

            return;
        }

        if (!ctx.level.getBlockState(hutPos).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("Expected ResidenceHut block at " + hutPos);

            return;
        }

        BuildingId createdId = valid.building().id();

        if (BuildingIndex.get(ctx.level).find(createdId).isEmpty())
        {
            helper.fail("BuildingIndex should contain the new building");

            return;
        }

        ctx.manager.confirm(ctx.playerUuid);

        if (ctx.manager.get(ctx.playerUuid).isPresent())
        {
            helper.fail("Pending placement should be cleared after success");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void paintingZoneNotContainingHutFails(GameTestHelper helper)
    {
        Context ctx = setup(helper);
        BlockPos hutPos = helper.absolutePos(new BlockPos(1, 2, 1));
        startPending(ctx, hutPos);

        BlockPos cornerA = hutPos.offset(10, 0, 10);
        BlockPos cornerB = hutPos.offset(14, 4, 14);

        BuildingPlacementService.Result result = BuildingPlacementService.get(ctx.level)
                .attemptPlacement(ctx.playerUuid, ctx.pending(), cornerA, cornerB, ColonyBlocks.RESIDENCE_HUT.get());

        if (!(result instanceof BuildingPlacementService.Result.Invalid))
        {
            helper.fail("Expected Invalid (DoesNotContainHutPos)");

            return;
        }

        if (ctx.level.getBlockState(hutPos).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("ResidenceHut block should not be placed on rejected zone");

            return;
        }

        if (ctx.manager.get(ctx.playerUuid).isEmpty())
        {
            helper.fail("Pending placement should remain active so the player can adjust");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void paintingOverlapsExistingBuildingFails(GameTestHelper helper)
    {
        Context ctx = setup(helper);
        BlockPos firstHut = helper.absolutePos(new BlockPos(1, 2, 1));

        AxisAlignedOuterZone existingZone = AxisAlignedOuterZone.fromCorners(
                firstHut.offset(-3, -1, -3),
                firstHut.offset(3, 3, 3));
        BuildingIndex.get(ctx.level).register(
                BuildingId.random(),
                new BuildingMetadata(ctx.colony, ResidenceHutType.INSTANCE, firstHut, existingZone));

        BlockPos secondHut = firstHut.offset(2, 0, 0);
        startPending(ctx, secondHut);

        BlockPos cornerA = secondHut.offset(-2, -1, -2);
        BlockPos cornerB = secondHut.offset(2, 2, 2);

        BuildingPlacementService.Result result = BuildingPlacementService.get(ctx.level)
                .attemptPlacement(ctx.playerUuid, ctx.pending(), cornerA, cornerB, ColonyBlocks.RESIDENCE_HUT.get());

        if (!(result instanceof BuildingPlacementService.Result.Invalid))
        {
            helper.fail("Expected Invalid (OverlapsExistingBuilding)");

            return;
        }

        if (ctx.level.getBlockState(secondHut).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("Overlap should prevent block placement");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void paintingZoneTooSmallFails(GameTestHelper helper)
    {
        Context ctx = setup(helper);
        BlockPos hutPos = helper.absolutePos(new BlockPos(1, 2, 1));
        startPending(ctx, hutPos);

        BlockPos cornerA = hutPos;
        BlockPos cornerB = hutPos.offset(1, 1, 1);

        BuildingPlacementService.Result result = BuildingPlacementService.get(ctx.level)
                .attemptPlacement(ctx.playerUuid, ctx.pending(), cornerA, cornerB, ColonyBlocks.RESIDENCE_HUT.get());

        if (!(result instanceof BuildingPlacementService.Result.Invalid))
        {
            helper.fail("Expected Invalid (TooSmall)");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    public static void paintingZoneTooLargeFails(GameTestHelper helper)
    {
        Context ctx = setup(helper);
        BlockPos hutPos = helper.absolutePos(new BlockPos(1, 2, 1));
        startPending(ctx, hutPos);

        BlockPos cornerA = hutPos.offset(-20, -10, -20);
        BlockPos cornerB = hutPos.offset(20, 10, 20);

        BuildingPlacementService.Result result = BuildingPlacementService.get(ctx.level)
                .attemptPlacement(ctx.playerUuid, ctx.pending(), cornerA, cornerB, ColonyBlocks.RESIDENCE_HUT.get());

        if (!(result instanceof BuildingPlacementService.Result.Invalid))
        {
            helper.fail("Expected Invalid (TooLarge)");

            return;
        }

        helper.succeed();
    }

    private static Context setup(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null)
        {
            helper.fail("GameTest requires a running server");

            throw new IllegalStateException("unreachable");
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        manager.clearAll();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        UUID playerUuid = player.getUUID();

        BlockPos townHall = helper.absolutePos(new BlockPos(0, 2, 0));
        ColonyId colony = TownHallPlacementListener.foundColonyAt(level, townHall, playerUuid, "TestColony");

        return new Context(level, server, playerUuid, colony, manager);
    }

    private static void startPending(Context ctx, BlockPos hutPos)
    {
        ctx.manager.start(
                ctx.playerUuid,
                ResidenceHutType.INSTANCE,
                hutPos,
                ctx.level.dimension(),
                ctx.server.getTickCount());
    }

    private record Context(
            ServerLevel level,
            MinecraftServer server,
            UUID playerUuid,
            ColonyId colony,
            PendingPlacementManager manager)
    {
        PendingPlacement pending()
        {
            return manager.get(playerUuid)
                    .orElseGet(() -> new PendingPlacement(
                            playerUuid,
                            ResidenceHutType.INSTANCE,
                            BlockPos.ZERO,
                            level.dimension(),
                            server.getTickCount(),
                            PendingPlacementState.AWAITING_PAINTING));
        }
    }
}
