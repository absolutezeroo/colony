package com.akikazu.colony.neoforge.client.screen;

import com.akikazu.colony.neoforge.network.OpenBuildingScreenPayload;
import com.akikazu.colony.neoforge.network.OpenScarecrowConfigurationPayload;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-side entry points for opening Colony's vanilla {@link net.minecraft.client.gui.screens.Screen} instances in
 * response to S2C payloads. Kept in {@code client.screen} so the dedicated-server tier never loads the class — payload
 * handlers in {@link com.akikazu.colony.neoforge.network.ColonyPayloads} are wired through this single seam.
 */
@OnlyIn(Dist.CLIENT)
public final class ColonyScreenOpener
{
    private ColonyScreenOpener()
    {
    }

    public static void openScarecrowConfiguration(OpenScarecrowConfigurationPayload payload)
    {
        Minecraft.getInstance().setScreen(new ScarecrowConfigurationScreen(payload.pos()));
    }

    public static void openBuildingScreen(OpenBuildingScreenPayload payload)
    {
        Minecraft.getInstance().setScreen(new BuildingScreen(payload));
    }
}
