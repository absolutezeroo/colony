package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.api.item.ColonyToolMode;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The multi-purpose tool the player receives at colony foundation. The cycled {@link ColonyToolMode} is persisted on
 * the {@link ItemStack} via {@link ColonyDataComponents#TOOL_MODE}; cycling itself happens client-side on shift+scroll
 * and is committed to the stack by the {@code CycleColonyToolModePayload} server handler.
 *
 * <p>
 * The {@link #use(Level, Player, InteractionHand)} branches by mode but emits placeholder chat messages — the real Zone
 * / Storage / Link behaviors land in prompts 2.3–2.4. Inspect already resolves a block via ray cast so the mode
 * detection is end-to-end testable in this prompt.
 */
public final class ColonyToolItem extends Item
{
    private static final double INSPECT_RAYCAST_RANGE = 20.0D;

    public ColonyToolItem(Properties properties)
    {
        super(properties);
    }

    public static ColonyToolMode getMode(ItemStack stack)
    {
        ColonyToolMode mode = stack.get(ColonyDataComponents.TOOL_MODE.get());

        if (mode == null)
        {
            return ColonyToolMode.DEFAULT;
        }

        return mode;
    }

    public static void setMode(ItemStack stack, ColonyToolMode mode)
    {
        stack.set(ColonyDataComponents.TOOL_MODE.get(), mode);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide())
        {
            return InteractionResultHolder.success(stack);
        }

        ColonyToolMode mode = getMode(stack);

        switch (mode)
        {
            case ZONE -> player.sendSystemMessage(
                    Component.literal("Zone painting will be available in prompt 2.3"));
            case STORAGE -> player.sendSystemMessage(
                    Component.literal("Storage typing will be available in prompt 2.4"));
            case LINK -> player.sendSystemMessage(
                    Component.literal("Anchor linking will be available in later prompt"));
            case INSPECT -> player.sendSystemMessage(describeLookedAtBlock(level, player));
            default -> player.sendSystemMessage(Component.literal("Unknown mode"));
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltip, flag);

        ColonyToolMode mode = getMode(stack);
        Component modeName = Component.translatable("item.colony.colony_tool.mode." + mode.getName());

        tooltip.add(Component.translatable("colony.gui.tooltip.colony_tool", modeName)
                .withStyle(ChatFormatting.GRAY));
    }

    private static Component describeLookedAtBlock(Level level, Player player)
    {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.x * INSPECT_RAYCAST_RANGE, view.y * INSPECT_RAYCAST_RANGE,
                view.z * INSPECT_RAYCAST_RANGE);

        BlockHitResult hit = level.clip(
                new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.MISS)
        {
            return Component.literal("Inspect: no block in range");
        }

        BlockPos pos = hit.getBlockPos();
        String blockName = level.getBlockState(pos).getBlock().getDescriptionId();

        return Component.literal("Inspect: " + blockName + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }
}
