package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.command.ColonyCommands;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.item.ColonyToolItem;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColonyToolGameTests
{
    private ColonyToolGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    public static void colonyToolModesCycleCorrectly(GameTestHelper helper)
    {
        ItemStack stack = new ItemStack(ColonyItems.COLONY_TOOL.get());

        if (ColonyToolItem.getMode(stack) != ColonyToolMode.ZONE)
        {
            helper.fail("Expected default mode ZONE, was " + ColonyToolItem.getMode(stack));

            return;
        }

        ColonyToolItem.setMode(stack, ColonyToolItem.getMode(stack).next());

        if (ColonyToolItem.getMode(stack) != ColonyToolMode.STORAGE)
        {
            helper.fail("Expected STORAGE after first next(), was " + ColonyToolItem.getMode(stack));

            return;
        }

        ColonyToolItem.setMode(stack, ColonyToolItem.getMode(stack).next());

        if (ColonyToolItem.getMode(stack) != ColonyToolMode.LINK)
        {
            helper.fail("Expected LINK after second next(), was " + ColonyToolItem.getMode(stack));

            return;
        }

        ColonyToolItem.setMode(stack, ColonyToolItem.getMode(stack).next());

        if (ColonyToolItem.getMode(stack) != ColonyToolMode.INSPECT)
        {
            helper.fail("Expected INSPECT after third next(), was " + ColonyToolItem.getMode(stack));

            return;
        }

        ColonyToolItem.setMode(stack, ColonyToolItem.getMode(stack).next());

        if (ColonyToolItem.getMode(stack) != ColonyToolMode.ZONE)
        {
            helper.fail("Expected ZONE to wrap from INSPECT, was " + ColonyToolItem.getMode(stack));

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3_platform")
    @SuppressWarnings({ "deprecation", "removal" })
    public static void colonyToolRecoveryCommand(GameTestHelper helper)
    {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CommandSourceStack source = player.createCommandSourceStack();

        if (ColonyCommands.playerHasColonyTool(player))
        {
            helper.fail("Mock player should start without a Colony Tool");

            return;
        }

        int firstResult = ColonyCommands.runWandRestore(source);

        if (firstResult != 1)
        {
            helper.fail("Expected first /colony wand restore to succeed (returned " + firstResult + ")");

            return;
        }

        if (!ColonyCommands.playerHasColonyTool(player))
        {
            helper.fail("Player should have a Colony Tool after /colony wand restore");

            return;
        }

        int countBefore = countColonyTools(player);
        int secondResult = ColonyCommands.runWandRestore(source);

        if (secondResult != 0)
        {
            helper.fail("Expected second /colony wand restore to be no-op (returned " + secondResult + ")");

            return;
        }

        int countAfter = countColonyTools(player);

        if (countAfter != countBefore)
        {
            helper.fail("Second /colony wand restore should not duplicate the tool; before="
                    + countBefore + ", after=" + countAfter);

            return;
        }

        helper.succeed();
    }

    private static int countColonyTools(ServerPlayer player)
    {
        int total = 0;

        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.is(ColonyItems.COLONY_TOOL.get()))
            {
                total += stack.getCount();
            }
        }

        for (ItemStack stack : player.getInventory().offhand)
        {
            if (stack.is(ColonyItems.COLONY_TOOL.get()))
            {
                total += stack.getCount();
            }
        }

        return total;
    }
}
