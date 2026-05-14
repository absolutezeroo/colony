package com.akikazu.colony.neoforge.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;

/**
 * The Residence Hut block. The first registered hut block type; sibling huts will follow the same shape in later
 * prompts.
 *
 * <p>
 * Unlike {@link TownHallBlock}, placement of this block is NOT triggered by the player dropping it directly. The
 * matching {@link com.akikazu.colony.neoforge.item.ResidenceHutBlockItem block item} intercepts the right-click and
 * enters the {@code PendingPlacement} state machine instead; the block itself is only placed once the painting workflow
 * confirms (prompt 2.3). Horizontal facing is preserved for the eventual facade model.
 */
public final class ResidenceHutBlock extends HorizontalDirectionalBlock
{
    public static final MapCodec<ResidenceHutBlock> CODEC = simpleCodec(ResidenceHutBlock::new);

    public ResidenceHutBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ResidenceHutBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit)
    {
        return InteractionResult.PASS;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return false;
    }
}
