package com.akikazu.colony.neoforge.block.event;

import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.common.citizen.spawn.CitizenSpawnConfig;
import com.akikazu.colony.common.citizen.spawn.CitizenSpawner;
import com.akikazu.colony.common.colony.ColonyManager;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;
import com.akikazu.colony.neoforge.entity.ColonyEntities;
import com.akikazu.colony.neoforge.item.ColonyItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;
import java.util.UUID;

/**
 * NeoForge listener that lifts a Town Hall block placement into the colony founding workflow.
 *
 * <p>
 * Subscribes on the {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS}. The intercepted event carries the placing
 * entity, which {@code Block#onPlace} would not, so the founder's UUID is available without resorting to a separate
 * lookup. The actual founding flow is exposed as {@link #foundColonyAt(ServerLevel, BlockPos, UUID, String)} so
 * gametests can drive it identically without staging a player.
 */
public final class TownHallPlacementListener
{
    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event)
    {
        LevelAccessor accessor = event.getLevel();

        if (!(accessor instanceof ServerLevel level))
        {
            return;
        }

        BlockState placed = event.getPlacedBlock();

        if (!placed.is(ColonyBlocks.TOWN_HALL.get()))
        {
            return;
        }

        Entity entity = event.getEntity();

        if (!(entity instanceof ServerPlayer player))
        {
            return;
        }

        BlockPos pos = event.getPos();
        String name = "Colony of " + player.getGameProfile().getName();

        foundColonyAt(level, pos.immutable(), player.getUUID(), name);

        player.sendSystemMessage(Component.translatable("colony.message.colony_founded", name));

        if (ColonyItems.COLONY_TOOL.isBound())
        {
            ItemStack tool = new ItemStack(ColonyItems.COLONY_TOOL.get());

            if (!player.getInventory().add(tool))
            {
                player.drop(tool, false);
            }
        }
    }

    public static ColonyId foundColonyAt(ServerLevel level, BlockPos townHallPos, UUID founder, String name)
    {
        ColonyManager manager = ColonyManager.get(level);
        ColonyId colonyId = manager.createColony(name, townHallPos, founder);

        List<EntityCitizen> citizens = CitizenSpawner.spawn(
                level,
                townHallPos,
                CitizenSpawnConfig.STARTING_CITIZEN_COUNT,
                colonyId,
                ColonyEntities.CITIZEN.get());

        List<CitizenId> ids = CitizenSpawner.idsOf(citizens);
        manager.registerInitialCitizens(colonyId, ids);

        ColonyMod.LOGGER.info(
                "Founded colony '{}' ({}) at {} for founder {}; immediately spawned {}/{} citizens.",
                name,
                colonyId,
                townHallPos,
                founder,
                citizens.size(),
                CitizenSpawnConfig.STARTING_CITIZEN_COUNT);

        return colonyId;
    }
}
