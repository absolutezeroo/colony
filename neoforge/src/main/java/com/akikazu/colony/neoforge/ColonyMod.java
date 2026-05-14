package com.akikazu.colony.neoforge;

import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.core.registry.RegistryView;
import com.akikazu.colony.neoforge.block.ColonyBlocks;
import com.akikazu.colony.neoforge.block.event.TownHallPlacementListener;
import com.akikazu.colony.neoforge.citizen.CitizenSpawnTicker;
import com.akikazu.colony.neoforge.client.ColonyClientEvents;
import com.akikazu.colony.neoforge.command.ColonyCommands;
import com.akikazu.colony.neoforge.data.FunctionalBlockDetectorReloadListener;
import com.akikazu.colony.neoforge.entity.ColonyEntities;
import com.akikazu.colony.neoforge.gametest.BuildingPlacementGameTests;
import com.akikazu.colony.neoforge.gametest.ColonyRegistrationGameTest;
import com.akikazu.colony.neoforge.gametest.ColonyToolGameTests;
import com.akikazu.colony.neoforge.gametest.EntityCitizenGameTests;
import com.akikazu.colony.neoforge.gametest.PendingPlacementGameTests;
import com.akikazu.colony.neoforge.gametest.TownHallFoundingGameTests;
import com.akikazu.colony.neoforge.item.ColonyCreativeTabs;
import com.akikazu.colony.neoforge.item.ColonyDataComponents;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.network.ColonyPayloads;
import com.akikazu.colony.neoforge.network.ColonyServerSession;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Entry point for the Colony mod on the NeoForge loader.
 *
 * <p>
 * Wires loader lifecycle events to colony bootstrap. Keeps logic minimal: heavier subsystems live in {@code :common}
 * and are reachable from this class only.
 */
@Mod(ColonyMod.MOD_ID)
public final class ColonyMod
{
    public static final String MOD_ID = "colony";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ColonyMod(IEventBus modEventBus)
    {
        ColonyBootstrap.register();

        ColonyEntities.register(modEventBus);
        ColonyBlocks.register(modEventBus);
        ColonyDataComponents.register(modEventBus);
        ColonyItems.register(modEventBus);
        ColonyCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onCreateAttributes);
        modEventBus.addListener(ColonyPayloads::register);
        modEventBus.addListener(this::onRegisterGameTests);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            ColonyClientEvents.register(modEventBus);
        }

        NeoForge.EVENT_BUS.addListener(ColonyCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.register(new TownHallPlacementListener());
        NeoForge.EVENT_BUS.register(new CitizenSpawnTicker());
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
    }

    private void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(new FunctionalBlockDetectorReloadListener());
    }

    private void onCreateAttributes(EntityAttributeCreationEvent event)
    {
        event.put(ColonyEntities.CITIZEN.get(), EntityCitizen.createAttributes().build());
    }

    private void onRegisterGameTests(RegisterGameTestsEvent event)
    {
        event.register(BuildingPlacementGameTests.class);
        event.register(ColonyRegistrationGameTest.class);
        event.register(ColonyToolGameTests.class);
        event.register(EntityCitizenGameTests.class);
        event.register(PendingPlacementGameTests.class);
        event.register(TownHallFoundingGameTests.class);
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        RegistryView<JobType> jobTypes = ColonyBootstrap.jobTypesView();
        String ids = jobTypes.values()
                .map(JobType::id)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        LOGGER.info("Colony loaded on server: {} JobType(s) registered [{}]", jobTypes.size(), ids);
    }

    private void onServerStopped(ServerStoppedEvent event)
    {
        MinecraftServer server = event.getServer();

        if (server != null)
        {
            ColonyServerSession.release(server);
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.sendSystemMessage(Component.literal("Hello, Colony is loaded."));
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();

        if (server == null)
        {
            return;
        }

        ColonyServerSession session = ColonyServerSession.get(server);
        session.subscriptions().forgetPlayer(serverPlayer.getUUID());
        session.toolCycleLimiter().forget(serverPlayer.getUUID());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        if (ColonyCommands.playerHasColonyTool(serverPlayer))
        {
            return;
        }

        ItemStack tool = new ItemStack(ColonyItems.COLONY_TOOL.get());

        if (!serverPlayer.getInventory().add(tool))
        {
            serverPlayer.drop(tool, false);
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();

        if (server == null)
        {
            return;
        }

        ColonyServerSession.get(server).subscriptions().forgetPlayer(serverPlayer.getUUID());
    }
}
