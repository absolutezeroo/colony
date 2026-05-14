package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.common.building.impl.ResidenceHutType;
import com.akikazu.colony.common.building.placement.PendingPlacement;
import com.akikazu.colony.common.building.placement.PendingPlacementManager;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.network.ColonyServerSession;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PendingPlacementGameTests
{
    private PendingPlacementGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    @SuppressWarnings({ "deprecation", "removal" })
    public static void rightClickResidenceHutEntersPendingState(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null)
        {
            helper.fail("GameTest requires a running server");

            return;
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        manager.clearAll();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        UUID uuid = player.getUUID();

        BlockPos floorPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);

        ItemStack hutStack = new ItemStack(ColonyItems.RESIDENCE_HUT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hutStack);

        giveColonyTool(player);

        InteractionResult result = simulateUseOn(level, player, hutStack, floorPos);

        if (result != InteractionResult.CONSUME)
        {
            helper.fail("Expected CONSUME from useOn, got " + result);

            return;
        }

        Optional<PendingPlacement> pending = manager.get(uuid);

        if (pending.isEmpty())
        {
            helper.fail("Expected pending placement to be registered after useOn");

            return;
        }

        BlockPos expectedTarget = floorPos.above();

        if (!pending.get().targetHutPos().equals(expectedTarget))
        {
            helper.fail("Pending target mismatch; expected " + expectedTarget + " got " + pending.get().targetHutPos());

            return;
        }

        if (pending.get().hutType() != ResidenceHutType.INSTANCE)
        {
            helper.fail("Pending hut type should be ResidenceHutType");

            return;
        }

        if (level.getBlockState(expectedTarget).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("ResidenceHut block should NOT be placed yet (pending state only)");

            return;
        }

        manager.clearAll();
        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    @SuppressWarnings({ "deprecation", "removal" })
    public static void escCancelsPendingPlacement(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null)
        {
            helper.fail("GameTest requires a running server");

            return;
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        manager.clearAll();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        UUID uuid = player.getUUID();

        BlockPos target = helper.absolutePos(new BlockPos(1, 2, 1));
        manager.start(uuid, ResidenceHutType.INSTANCE, target, level.dimension(), server.getTickCount());

        Optional<PendingPlacement> removed = manager.cancel(uuid);

        if (removed.isEmpty())
        {
            helper.fail("cancel() should have returned the active pending");

            return;
        }

        if (manager.get(uuid).isPresent())
        {
            helper.fail("Pending placement should be empty after cancel");

            return;
        }

        if (level.getBlockState(target).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("Cancelled placement must not leave a ResidenceHut block in the world");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    @SuppressWarnings({ "deprecation", "removal" })
    public static void rightClickWithoutColonyToolRejected(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();

        if (server == null)
        {
            helper.fail("GameTest requires a running server");

            return;
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        manager.clearAll();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        BlockPos floorPos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(floorPos, Blocks.STONE.defaultBlockState(), 3);

        ItemStack hutStack = new ItemStack(ColonyItems.RESIDENCE_HUT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hutStack);

        InteractionResult result = simulateUseOn(level, player, hutStack, floorPos);

        if (result != InteractionResult.FAIL)
        {
            helper.fail("Expected FAIL when player has no Colony Tool, got " + result);

            return;
        }

        if (manager.get(player.getUUID()).isPresent())
        {
            helper.fail("Pending placement should not exist when player lacks a Colony Tool");

            return;
        }

        if (level.getBlockState(floorPos.above()).is(ColonyBlocks.RESIDENCE_HUT.get()))
        {
            helper.fail("ResidenceHut block must not be placed when Colony Tool is missing");

            return;
        }

        helper.succeed();
    }

    private static InteractionResult simulateUseOn(
            ServerLevel level,
            ServerPlayer player,
            ItemStack stack,
            BlockPos clickedPos)
    {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(clickedPos.above()),
                Direction.UP,
                clickedPos,
                false);
        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit);

        return stack.getItem().useOn(context);
    }

    private static void giveColonyTool(ServerPlayer player)
    {
        ItemStack tool = new ItemStack(ColonyItems.COLONY_TOOL.get());

        if (!player.getInventory().add(tool))
        {
            player.drop(tool, false);
        }
    }
}
