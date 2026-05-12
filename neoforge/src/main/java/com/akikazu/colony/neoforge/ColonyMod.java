package com.akikazu.colony.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Colony mod on the NeoForge loader.
 *
 * <p>Wires loader lifecycle events to colony bootstrap. Keeps logic minimal: heavier
 * subsystems live in {@code :common} and are reachable from this class only.
 */
@Mod(ColonyMod.MOD_ID)
public final class ColonyMod
{
    public static final String MOD_ID = "colony";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ColonyMod(IEventBus modEventBus)
    {
        modEventBus.addListener(this::onCommonSetup);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
    }

    private void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Colony loaded on server");
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.sendSystemMessage(Component.literal("Hello, Colony is loaded."));
        }
    }
}
