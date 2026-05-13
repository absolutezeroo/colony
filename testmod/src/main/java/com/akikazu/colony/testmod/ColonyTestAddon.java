package com.akikazu.colony.testmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder addon that depends only on {@code :api}. Its purpose is to keep the SPI honest — if anything in this
 * module fails to compile against the published API surface, the offending change is rejected at build time before it
 * can land in {@code :common} or {@code :neoforge}.
 */
@Mod(ColonyTestAddon.MOD_ID)
public final class ColonyTestAddon
{
    public static final String MOD_ID = "colonytest";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ColonyTestAddon(IEventBus modEventBus)
    {
        LOGGER.info("Colony test addon loaded");
    }
}
