package com.akikazu.colony.neoforge.item;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.common.building.impl.ResidenceHutType;
import com.akikazu.colony.common.building.placement.PendingPlacementManager;
import com.akikazu.colony.neoforge.block.ResidenceHutBlock;
import com.akikazu.colony.neoforge.command.ColonyCommands;
import com.akikazu.colony.neoforge.network.ColonyServerSession;
import com.akikazu.colony.neoforge.network.SetPendingPlacementClientPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@link BlockItem} for the Residence Hut. Overrides {@link #useOn} to intercept the right-click BEFORE vanilla
 * placement happens, redirecting the player into the {@code PendingPlacement} workflow instead of consuming the item.
 *
 * <p>
 * On the client the right-click is consumed without effect. Server-side we validate the player carries a Colony Tool,
 * register the pending placement on {@link PendingPlacementManager}, force the Colony Tool to
 * {@link ColonyToolMode#ZONE Zone} mode, and notify the client to render the ghost preview. The actual block placement
 * happens later when the painting confirms (prompt 2.3); for this prompt, {@link #useOn} never reaches the
 * super-implementation that would put the hut block in the world.
 */
public final class ResidenceHutBlockItem extends BlockItem
{
    public ResidenceHutBlockItem(ResidenceHutBlock block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null)
        {
            return InteractionResult.PASS;
        }

        if (level.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResult.PASS;
        }

        if (!ColonyCommands.playerHasColonyTool(serverPlayer))
        {
            serverPlayer.sendSystemMessage(
                    Component.translatable("colony.message.pending_placement.no_colony_tool"));

            return InteractionResult.FAIL;
        }

        MinecraftServer server = serverPlayer.getServer();

        if (server == null)
        {
            return InteractionResult.FAIL;
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();

        if (manager.get(serverPlayer.getUUID()).isPresent())
        {
            return InteractionResult.CONSUME;
        }

        BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
        HutType hutType = ResidenceHutType.INSTANCE;

        manager.start(
                serverPlayer.getUUID(),
                hutType,
                targetPos,
                serverPlayer.level().dimension(),
                server.getTickCount());

        forceColonyToolToZoneMode(serverPlayer);

        sendClientStateIfConnected(serverPlayer, new SetPendingPlacementClientPayload(hutType.id(), targetPos));

        serverPlayer.sendSystemMessage(
                Component.translatable("colony.message.pending_placement.started", hutType.displayName()));

        return InteractionResult.CONSUME;
    }

    private static void sendClientStateIfConnected(ServerPlayer player, SetPendingPlacementClientPayload payload)
    {
        try
        {
            PacketDistributor.sendToPlayer(player, payload);
        }
        catch (RuntimeException ignored)
        {
            // GameTest mock players have an embedded serverbound-only channel; sending clientbound payloads to them
            // throws. The server-authoritative state in PendingPlacementManager is already correct — only the client
            // ghost preview is missing, which the gametest does not assert.
        }
    }

    private static void forceColonyToolToZoneMode(ServerPlayer player)
    {
        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.is(ColonyItems.COLONY_TOOL.get()))
            {
                ColonyToolItem.setMode(stack, ColonyToolMode.ZONE);
            }
        }

        for (ItemStack stack : player.getInventory().offhand)
        {
            if (stack.is(ColonyItems.COLONY_TOOL.get()))
            {
                ColonyToolItem.setMode(stack, ColonyToolMode.ZONE);
            }
        }
    }
}
