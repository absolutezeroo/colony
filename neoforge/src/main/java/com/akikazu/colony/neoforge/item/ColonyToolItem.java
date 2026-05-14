package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.common.storage.impl.SlotSelectionContext;
import com.akikazu.colony.common.storage.impl.SlotSelectionManager;
import com.akikazu.colony.common.workzone.impl.AnchorIndex;
import com.akikazu.colony.common.workzone.impl.AnchorMetadata;
import com.akikazu.colony.common.workzone.impl.AnchorSelectionContext;
import com.akikazu.colony.common.workzone.impl.AnchorSelectionManager;
import com.akikazu.colony.neoforge.network.ColonyServerSession;
import com.akikazu.colony.neoforge.storage.ChestTypingDispatcher;
import com.akikazu.colony.neoforge.workzone.AnchorLinkingDispatcher;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The multi-purpose tool the player receives at colony foundation. The cycled {@link ColonyToolMode} is persisted on
 * the {@link ItemStack} via {@link ColonyDataComponents#TOOL_MODE}; cycling itself happens client-side on shift+scroll
 * and is committed to the stack by the {@code CycleColonyToolModePayload} server handler.
 *
 * <p>
 * Right-click dispatch splits across {@link #useOn(UseOnContext)} and {@link #use(Level, Player, InteractionHand)}:
 * {@code useOn} handles cases that target a specific block — STORAGE mode types the clicked chest, LINK mode links the
 * clicked anchor to a previously-armed building — while {@code use} runs when no block is targeted (and surfaces hints
 * for modes that have nothing to do without a target).
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
    public InteractionResult useOn(UseOnContext context)
    {
        ItemStack stack = context.getItemInHand();
        ColonyToolMode mode = getMode(stack);

        return switch (mode)
        {
            case STORAGE -> useOnForStorage(context);
            case LINK -> useOnForLink(context);
            default -> InteractionResult.PASS;
        };
    }

    private static InteractionResult useOnForStorage(UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide())
        {
            if (level.getBlockState(pos).getBlock() instanceof ChestBlock)
            {
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
        {
            return InteractionResult.PASS;
        }

        if (!(serverLevel.getBlockState(pos).getBlock() instanceof ChestBlock))
        {
            return InteractionResult.PASS;
        }

        handleStorageClick(serverLevel, serverPlayer, pos);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useOnForLink(UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel))
        {
            return InteractionResult.PASS;
        }

        handleLinkClick(serverLevel, serverPlayer, pos);

        return InteractionResult.SUCCESS;
    }

    private static void handleStorageClick(ServerLevel level, ServerPlayer player, BlockPos pos)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        SlotSelectionManager selections = ColonyServerSession.get(server).slotSelections();
        Optional<SlotSelectionContext> selectionOpt = selections.current(player.getUUID(), server.getTickCount());

        if (selectionOpt.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("colony.message.storage.no_selection"));

            return;
        }

        SlotSelectionContext selection = selectionOpt.get();

        ChestTypingDispatcher.assign(level, player, pos, selection.building(), selection.slotId());

        selections.clear(player.getUUID());
    }

    private static void handleLinkClick(ServerLevel level, ServerPlayer player, BlockPos pos)
    {
        MinecraftServer server = player.getServer();

        if (server == null)
        {
            return;
        }

        Optional<Map.Entry<AnchorId, AnchorMetadata>> anchorOpt = AnchorIndex.get(level).findByPosition(pos);

        if (anchorOpt.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("colony.message.anchor.no_anchor_here"));

            return;
        }

        AnchorId anchorId = anchorOpt.get().getKey();
        AnchorMetadata anchorMeta = anchorOpt.get().getValue();

        AnchorSelectionManager selections = ColonyServerSession.get(server).anchorSelections();
        Optional<AnchorSelectionContext> selectionOpt = selections.current(player.getUUID(), server.getTickCount());

        if (selectionOpt.isEmpty())
        {
            if (anchorMeta.linkedBuilding().isPresent())
            {
                player.sendSystemMessage(Component.translatable(
                        "colony.message.anchor.already_linked_hint",
                        anchorMeta.linkedBuilding().get().toString().substring(0, 8)));
            }
            else
            {
                player.sendSystemMessage(Component.translatable("colony.message.anchor.no_selection"));
            }

            return;
        }

        AnchorSelectionContext selection = selectionOpt.get();

        AnchorLinkingDispatcher.link(level, player, anchorId, selection.building(), selection.slotId());

        selections.clear(player.getUUID());
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
            case ZONE -> handleZoneUse(player);
            case STORAGE -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.no_selection"));
            case LINK -> player.sendSystemMessage(
                    Component.translatable("colony.message.link.target_required"));
            case INSPECT -> player.sendSystemMessage(describeLookedAtBlock(level, player));
        }

        return InteractionResultHolder.success(stack);
    }

    private static void handleZoneUse(Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();

        if (server == null)
        {
            return;
        }

        boolean hasPending = ColonyServerSession.get(server)
                .pendingPlacements()
                .get(serverPlayer.getUUID())
                .isPresent();

        if (hasPending)
        {
            return;
        }

        serverPlayer.sendSystemMessage(
                Component.translatable("colony.message.zone_mode.no_pending_placement"));
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
            return Component.translatable("colony.message.colony_tool.inspect.no_block");
        }

        BlockPos pos = hit.getBlockPos();
        Component blockName = Component.translatable(level.getBlockState(pos).getBlock().getDescriptionId());

        return Component.translatable(
                "colony.message.colony_tool.inspect.block",
                blockName,
                pos.getX(),
                pos.getY(),
                pos.getZ());
    }
}
