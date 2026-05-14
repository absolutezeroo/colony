package com.akikazu.colony.neoforge.block;

import com.akikazu.colony.api.workzone.AnchorConfigurationResult;
import com.akikazu.colony.common.workzone.scarecrow.ScarecrowAnchorType;
import com.akikazu.colony.neoforge.blockentity.ScarecrowBlockEntity;
import com.akikazu.colony.neoforge.network.OpenScarecrowConfigurationPayload;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The Scarecrow anchor block. Plain cuboid for V1 — a placeholder model (pumpkin on a fence) ships alongside as a model
 * JSON, but the block itself stays geometry-neutral so future tier-driven visual variants attach via state properties
 * without changing the runtime contract.
 *
 * <p>
 * Right-click dispatch:
 * <ul>
 * <li>Empty hand → open the configuration screen client-side via {@link OpenScarecrowConfigurationPayload}.</li>
 * <li>Item in hand accepted by {@link ScarecrowAnchorType#configurationHandler()} → apply, consume one, chat
 * feedback.</li>
 * <li>Any other item → {@link ItemInteractionResult#SKIP_DEFAULT_BLOCK_INTERACTION} so the item's normal placement
 * logic runs (e.g. placing a block adjacent to the scarecrow).</li>
 * </ul>
 */
public final class ScarecrowBlock extends Block implements EntityBlock
{
    public static final MapCodec<ScarecrowBlock> CODEC = simpleCodec(ScarecrowBlock::new);

    public ScarecrowBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<ScarecrowBlock> codec()
    {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ScarecrowBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit)
    {
        if (stack.isEmpty())
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!ScarecrowAnchorType.INSTANCE.configurationHandler().accepts(stack))
        {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide())
        {
            return ItemInteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer))
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Optional<ScarecrowBlockEntity> beOpt = beAt(serverLevel, pos);

        if (beOpt.isEmpty())
        {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        AnchorConfigurationResult result = beOpt.get().applyConfiguration(stack);

        switch (result)
        {
            case AnchorConfigurationResult.Applied applied ->
            {
                serverPlayer.sendSystemMessage(applied.feedbackMessage());

                if (applied.consumeOneItem() && !serverPlayer.getAbilities().instabuild)
                {
                    stack.shrink(1);
                }
            }
            case AnchorConfigurationResult.Rejected rejected ->
                serverPlayer.sendSystemMessage(rejected.errorMessage());
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit)
    {
        if (level.isClientSide())
        {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return net.minecraft.world.InteractionResult.PASS;
        }

        PacketDistributor.sendToPlayer(serverPlayer, new OpenScarecrowConfigurationPayload(pos));

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    private static Optional<ScarecrowBlockEntity> beAt(ServerLevel level, BlockPos pos)
    {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ScarecrowBlockEntity scarecrow)
        {
            return Optional.of(scarecrow);
        }

        return Optional.empty();
    }
}
